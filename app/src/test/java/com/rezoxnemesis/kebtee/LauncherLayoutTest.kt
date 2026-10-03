package com.rezoxnemesis.kebtee

import com.rezoxnemesis.kebtee.wallpaper.WallpaperScene
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLayoutTest {
    private val a = "com.example.a/Main"
    private val b = "com.example.b/Main"
    private val c = "com.example.c/Main"
    private fun app(key: String) = HomeItem(key, listOf(key))

    @Test fun normalizingUntrustedSettingsBoundsListsAndUsesSupportedChoices() {
        val layout = LauncherLayout(pages = emptyList(), dock = listOf(a, a, "invalid", b),
            hidden = setOf(a, "invalid"), columns = 99, iconSize = -1,
            drawerStyle = "bad", folderStyle = "bad", swipeDown = "bad", doubleTap = "bad").normalized()
        assertEquals(listOf(emptyList<HomeItem>()), layout.pages)
        assertEquals(listOf(a, b), layout.dock)
        assertEquals(setOf(a), layout.hidden)
        assertEquals(6, layout.columns)
        assertEquals(40, layout.iconSize)
        assertEquals("Grid", layout.drawerStyle)
        assertEquals("Glass", layout.folderStyle)
        assertEquals("Search", layout.swipeDown)
        assertEquals("Search", layout.doubleTap)
    }

    @Test fun normalizeDeduplicatesAcrossFoldersAndPagesWithoutRemovingHiddenMembership() {
        val layout = LauncherLayout(pages = listOf(
            listOf(HomeItem("folder:first", listOf(a, b, a, "invalid"), "x".repeat(100))),
            listOf(app(a), app(c))), hidden = setOf(b)).normalized()
        assertEquals(listOf(a, b, c), layout.pages.flatten().flatMap { it.apps })
        assertEquals(48, layout.pages.first().first().name.length)
        assertEquals(setOf(b), layout.hidden)
        assertEquals(layout, layout.normalized())
    }

    @Test fun placeMovesAnAppOutOfFolderAndDockWithoutDuplicatingIt() {
        val layout = LauncherLayout(pages = listOf(listOf(HomeItem("folder:a", listOf(a, b))), emptyList()), dock = listOf(a, c))
        val moved = layout.place(a, 1)
        assertEquals(listOf(app(b)), moved.pages[0])
        assertEquals(listOf(app(a)), moved.pages[1])
        assertEquals(listOf(c), moved.dock)
        assertEquals(1, moved.pages.flatten().flatMap { it.apps }.count { it == a })
        assertEquals(listOf(a, c), layout.dock)
    }

    @Test fun normalizationRemovesDockMembersFromHomePlacement() {
        val layout = LauncherLayout(
            pages = listOf(listOf(app(a), HomeItem("folder:work", listOf(b, a), "Work"))),
            dock = listOf(a)
        ).normalized()

        assertEquals(listOf(a), layout.dock)
        assertEquals(listOf(listOf(app(b))), layout.pages)
        assertFalse(layout.pages.flatten().flatMap { it.apps }.contains(a))
    }

    @Test fun removalDissolvesSingletonFolderAndRemovesEmptyItems() {
        val layout = LauncherLayout(pages = listOf(listOf(HomeItem("folder:a", listOf(a, b)), app(c))), dock = listOf(a, c))
        val removed = layout.remove(a).remove(c)
        assertEquals(listOf(listOf(app(b))), removed.pages)
        assertTrue(removed.dock.isEmpty())
    }

    @Test fun groupingMovesSourceIntoTargetAndPreservesMemberOrder() {
        val layout = LauncherLayout(pages = listOf(listOf(app(a), app(b)), listOf(app(c))), dock = listOf(c))
        val grouped = layout.group(b, a)
        val folder = grouped.pages.first().single()
        assertTrue(folder.isFolder)
        assertEquals(listOf(a, b), folder.apps)
        val extended = grouped.group(c, folder.id)
        assertEquals(listOf(a, b, c), extended.pages.first().single().apps)
        assertTrue(extended.pages[1].isEmpty())
        assertTrue(extended.dock.isEmpty())
        assertEquals(extended, extended.group(a, folder.id))
    }

    @Test fun lockedLayoutRejectsMovesAndGroupingAndMissingTargetsDoNotLoseApps() {
        val layout = LauncherLayout(pages = listOf(listOf(app(a), app(b))), locked = true)
        assertEquals(layout, layout.place(a, 0))
        assertEquals(layout, layout.group(b, a))
        val unlocked = layout.copy(locked = false)
        assertEquals(unlocked, unlocked.group(a, "missing"))
        assertEquals(unlocked, unlocked.place("invalid", 0))
    }

    @Test fun appKeysRequireExactlyOneNonEmptyComponentSeparator() {
        assertTrue(LauncherLayout.validAppKey(a))
        listOf("", "invalid", "/Main", "package/", "a/b/c", " /Main", "package/ ").forEach {
            assertFalse(it, LauncherLayout.validAppKey(it))
        }
    }
    @Test fun fullPageAndFolderRejectMovesWithoutLosingTheSource() {
        val full = (0 until 100).map { app("com.example.app$it/Main") }
        val pages = LauncherLayout(pages = listOf(full, listOf(app(a))), dock = listOf(a))
        assertEquals(pages, pages.place(a, 0))
        val folder = HomeItem("folder:full", full.map { it.id })
        val grouped = LauncherLayout(pages = listOf(listOf(folder, app(a))), dock = listOf(a))
        assertEquals(grouped, grouped.group(a, folder.id))
    }

    @Test fun movingIntoAnEmptyLayoutCreatesTheFirstPage() {
        assertEquals(listOf(listOf(app(a))), LauncherLayout(pages = emptyList()).place(a, 0).pages)
    }

    @Test fun normalizationRepairsIdsWithoutHidingAppsOrDuplicatingFolderTargets() {
        val layout = LauncherLayout(pages = listOf(listOf(
            HomeItem("wrong", listOf(a)), HomeItem("folder:same", listOf(b)),
            HomeItem("folder:same", listOf(c))))).normalized()
        assertEquals(a, layout.pages[0][0].id)
        assertEquals(3, layout.pages.flatten().map { it.id }.distinct().size)
        assertEquals(layout, layout.normalized())
        val multi = LauncherLayout(pages = listOf(listOf(HomeItem(a, listOf(a, b))))).normalized()
        assertTrue(multi.pages[0][0].isFolder)
        assertEquals(listOf(a, b), multi.pages[0][0].apps)
    }

    @Test fun normalizationDoesNotConsumeMembersBeyondFolderCapacity() {
        val members = (0 until 100).map { "com.example.app$it/Main" }
        val layout = LauncherLayout(pages = listOf(listOf(
            HomeItem("folder:large", members + a), app(a)))).normalized()
        assertEquals(101, layout.pages.flatten().flatMap { it.apps }.size)
        assertEquals(app(a), layout.pages[0][1])
    }

    @Test fun regroupingOriginalFolderAnchorCreatesDistinctFolderIds() {
        val d = "com.example.d/Main"
        val layout = LauncherLayout(pages = listOf(listOf(
            HomeItem("folder:$a", listOf(b, c)), app(a), app(d))))
        val grouped = layout.group(d, a)
        assertEquals(2, grouped.pages[0].size)
        assertEquals(2, grouped.pages[0].map { it.id }.distinct().size)
        assertEquals(listOf(a, d), grouped.pages[0][1].apps)
    }

    @Test fun groupingIntoSingletonFolderPreservesSourceAndFolderMetadata() {
        val folder = HomeItem("folder:kept", listOf(a), "Work", true)
        val layout = LauncherLayout(pages = listOf(listOf(folder, app(b))))
        val grouped = layout.group(b, folder.id)
        assertEquals(listOf(listOf(folder.copy(apps = listOf(a, b)))), grouped.pages)
        assertEquals(layout, layout.remove(c))
    }

    @Test fun backupRoundTripPreservesTiltMotionSetting() {
        val data = LauncherBackupData(
            layout = LauncherLayout(pages = listOf(listOf(app(a)))),
            wallpaper = WallpaperSettings(tiltMotion = false)
        )
        val decoded = LauncherBackupCodec.decode(LauncherBackupCodec.encode(data))
        assertFalse(decoded.wallpaper.tiltMotion)
    }

    @Test fun backupRoundTripPreserves120FpsWallpaperSetting() {
        val data = LauncherBackupData(
            layout = LauncherLayout(pages = listOf(listOf(app(a)))),
            wallpaper = WallpaperSettings(fps = 120)
        )
        val decoded = LauncherBackupCodec.decode(LauncherBackupCodec.encode(data))
        assertEquals(120, decoded.wallpaper.fps)
        assertEquals(data.layout.normalized(), decoded.layout)
    }

    @Test fun backupRoundTripPreservesSelectedWallpaperSceneAndOldBackupsStayCompatible() {
        val data = LauncherBackupData(
            layout = LauncherLayout(pages = listOf(listOf(app(a)))),
            wallpaper = WallpaperSettings(scene = WallpaperScene.AURA_PULSE)
        )
        val encoded = LauncherBackupCodec.encode(data)
        assertEquals(WallpaperScene.AURA_PULSE, LauncherBackupCodec.decode(encoded).wallpaper.scene)

        val legacy = JSONObject(encoded).apply { getJSONObject("wallpaper").remove("scene") }.toString()
        assertEquals(WallpaperScene.ORIGINAL, LauncherBackupCodec.decode(legacy).wallpaper.scene)
    }

}
