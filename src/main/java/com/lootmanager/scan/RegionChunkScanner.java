package com.lootmanager.scan;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;

final class RegionChunkScanner {

    private final File[] regionFiles;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private int regionFileIndex;
    private int regionX;
    private int regionZ;
    private int locationIndex;
    private byte[] locations;

    RegionChunkScanner(File worldFolder, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) throws IOException {
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkZ = maxChunkZ;
        File regionDirectory = new File(worldFolder, "region");
        if (!regionDirectory.exists()) {
            regionFiles = new File[0];
            return;
        }
        if (!regionDirectory.isDirectory()) {
            throw new IOException("World region path is not a directory: " + regionDirectory);
        }
        File[] files = regionDirectory.listFiles((directory, name) -> name.matches("r\\.-?\\d+\\.-?\\d+\\.mca"));
        if (files == null) {
            throw new IOException("Could not list region files in " + regionDirectory);
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        regionFiles = files;
    }

    ChunkCoordinate nextChunk() throws IOException {
        while (true) {
            if (locations == null || locationIndex >= 1024) {
                if (!loadNextRegionFile()) {
                    return null;
                }
            }
            int currentIndex = locationIndex++;
            int headerOffset = currentIndex * 4;
            if (locations[headerOffset] == 0 && locations[headerOffset + 1] == 0
                    && locations[headerOffset + 2] == 0) {
                continue;
            }
            long chunkX = (long) regionX * 32L + currentIndex % 32;
            long chunkZ = (long) regionZ * 32L + currentIndex / 32;
            if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ) {
                continue;
            }
            return new ChunkCoordinate((int) chunkX, (int) chunkZ);
        }
    }

    private boolean loadNextRegionFile() throws IOException {
        if (regionFileIndex >= regionFiles.length) {
            locations = null;
            return false;
        }
        File file = regionFiles[regionFileIndex++];
        String[] coordinates = file.getName().substring(2, file.getName().length() - 4).split("\\.");
        try {
            regionX = Integer.parseInt(coordinates[0]);
            regionZ = Integer.parseInt(coordinates[1]);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid region filename: " + file.getName(), exception);
        }
        if (file.length() < 4096L) {
            throw new IOException("Region file header is incomplete: " + file);
        }
        locations = new byte[4096];
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            input.readFully(locations);
        }
        locationIndex = 0;
        return true;
    }

    static final class ChunkCoordinate {
        final int x;
        final int z;

        ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}