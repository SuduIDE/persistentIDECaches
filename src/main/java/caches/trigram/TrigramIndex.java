package caches.trigram;

import caches.Index;
import caches.changes.*;
import caches.records.Revision;
import caches.records.Trigram;
import caches.records.TrigramFile;

import java.util.List;
import java.util.stream.Stream;

import static caches.GlobalVariables.revisions;

public class TrigramIndex implements Index<TrigramFile, Integer> {

    public final TrigramCache cache = new TrigramCache();
    public final Preparer preparer = new Preparer();
    public TrigramFileCounter counter = new TrigramFileCounter();

    public TrigramIndex() {}

    private static TrigramCounter getTrigramsCount(String str) {
        int[] codePoints = str.codePoints().toArray();
        TrigramCounter result = new TrigramCounter();
        for (int i = 0; i < codePoints.length - 3; i++) {
            Trigram trigram = new Trigram(new String(codePoints, i ,3));
            result.add(trigram);
        }
        return result;
    }

    @Override
    public void prepare(List<Change> changes) {
        preparer.process(changes);
    }

    @Override
    public void processChanges(List<Change> changes) {
        preparer.process(changes);
    }

    private void pushActions(TrigramFileCounter deltas, long timestamp) {
        cache.pushCluster(timestamp, deltas);
    }

    @Override
    public Integer getValue(TrigramFile trigramFile, Revision revision) {
        var currentRevision = revisions.getCurrentRevision();
        if (revision.equals(currentRevision)) {
            return counter.get(trigramFile.file(), trigramFile.trigram());
        } else {
            checkout(revision);
            var ans = counter.get(trigramFile.file(), trigramFile.trigram());
            checkout(currentRevision);
            return ans;
        }
    }

    @Override
    public void checkout(Revision targetRevision) {
        var currentRevision = revisions.getCurrentRevision();
        var targetCounter = counter.copy();
        while (!currentRevision.equals(targetRevision)) {
            if (currentRevision.revision() > targetRevision.revision()) {
                targetCounter.decrease(cache.getDataCluster(currentRevision));
                currentRevision = revisions.getParent(currentRevision);
            } else {
                targetCounter.add(cache.getDataCluster(targetRevision));
                targetRevision = revisions.getParent(targetRevision);
            }
        }
        counter = targetCounter;
    }


    private class Preparer {

        public void process(List<Change> changes) {
            var delta = new TrigramFileCounter();
            changes.forEach(it -> countChange(it, delta));
            counter.add(delta);
            if (!changes.isEmpty()) pushActions(delta, changes.get(0).getTimestamp());
        }

        private boolean validateFilename(String filename) {
            return Stream.of(".java"/*, ".txt", ".kt", ".py"*/).anyMatch(filename::endsWith);
        }

        private boolean validateChange(Change change) {
            List<String> filenames = switch (change) {
                case FileChange fileChange -> List.of(fileChange.getPlace().file().getName());
                case FileHolderChange fileHolderChange -> List.of(fileHolderChange.getOldFileName().getName(),
                        fileHolderChange.getNewFileName().getName());
            };
            return filenames.stream().anyMatch(this::validateFilename);
        }

        private void countChange(Change change, TrigramFileCounter delta) {
            if (!validateChange(change)) return;
            switch (change) {
                case AddChange addChange ->
                        delta.add(addChange.getPlace().file(), getTrigramsCount(addChange.getAddedString()));
                case ModifyChange modifyChange -> {
                    delta.decrease(modifyChange.getOldFileName(), getTrigramsCount(modifyChange.getOldFileContent()));
                    delta.add(modifyChange.getNewFileName(), getTrigramsCount(modifyChange.getNewFileContent()));
                }
                case CopyChange copyChange ->
                        delta.add(copyChange.getNewFileName(), getTrigramsCount(copyChange.getNewFileContent()));
                case RenameChange renameChange -> {
                    delta.decrease(renameChange.getOldFileName(), getTrigramsCount(renameChange.getOldFileContent()));
                    delta.add(renameChange.getNewFileName(), getTrigramsCount(renameChange.getNewFileContent()));
                }
                case DeleteChange deleteChange ->
                        delta.add(deleteChange.getPlace().file(), getTrigramsCount(deleteChange.getDeletedString()));
            }
        }
    }
}
