package caches.trigram;

import caches.GlobalVariables;
import caches.lmdb.LmdbInt2Long;
import caches.records.Revision;

import java.io.*;


public class TrigramCache {
    public static final String DIRECTORY = ".trigrams/";
    public static final File DATA_FILE = new File(DIRECTORY + ".data");
    private final LmdbInt2Long pointers = new LmdbInt2Long(GlobalVariables.env, "trigram_pointers");

    public void pushCluster(long timestamp, TrigramFileCounter deltas) {
        var revision = GlobalVariables.revisions.getCurrentRevision();
        long size = DATA_FILE.length();
        pointers.put(revision.revision(), size);
        try (FileOutputStream writer = new FileOutputStream(DATA_FILE, true)) {
            writer.write(new TrigramDataFileCluster(deltas).toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TrigramFileCounter getDataCluster(Revision revision) {
        long pointer = pointers.get(revision.revision());
        if (pointer == -1) {
            return TrigramFileCounter.EMPTY_COUNTER;
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(DATA_FILE, "r")) {
            randomAccessFile.seek(pointer);
            var bufferedInputStream = new BufferedInputStream(new FileInputStream(randomAccessFile.getFD()));
            TrigramDataFileCluster cluster = TrigramDataFileCluster.readTrigramDataFileCluster(bufferedInputStream);
            return cluster.deltas();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}