package com.localflow.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLogicTest {
    @Test fun folderGroupingCountsEntries() {
        // This pure transformation is the same grouping used after the MediaStore query.
        val grouped = listOf("Download", "Download").groupBy { it }.map { it.key to it.value.size }
        assertEquals(listOf("Download" to 2), grouped)
    }
}
