package com.lootmanager.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegionChunkScannerTest {

    @TempDir
    Path temporaryFolder;

    @Test
    void shouldFindSavedChunksWithinBoundsIncludingNegativeCoordinates() throws IOException {
        Path regionDirectory = Files.createDirectories(temporaryFolder.resolve("region"));
        byte[] header = new byte[4096];
        markChunkPresent(header, 30, 1);
        markChunkPresent(header, 31, 2);
        markChunkPresent(header, 29, 1);
        Files.write(regionDirectory.resolve("r.-1.0.mca"), header);

        RegionChunkScanner scanner = new RegionChunkScanner(temporaryFolder.toFile(), -2, 5, 0, 8);

        RegionChunkScanner.ChunkCoordinate first = scanner.nextChunk();
        assertEquals(-2, first.x);
        assertEquals(1, first.z);
        RegionChunkScanner.ChunkCoordinate second = scanner.nextChunk();
        assertEquals(-1, second.x);
        assertEquals(2, second.z);
        assertNull(scanner.nextChunk());
    }

    private void markChunkPresent(byte[] header, int localX, int localZ) {
        int offset = (localZ * 32 + localX) * 4;
        header[offset + 2] = 1;
        header[offset + 3] = 1;
    }
}