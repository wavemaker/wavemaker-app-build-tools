package com.wavemaker.app.build.project.model;

import java.util.List;

import com.wavemaker.commons.io.Folder;

/**
 * Created by kishore on 16/3/17.
 */
public class AppPackageConfig {

    private Folder basedir;
    private Folder targetDir;
    private String ignorePatternFile;
    private List<String> extraIgnorePatterns;

    public Folder getBasedir() {
        return basedir;
    }

    public Folder getTargetDir() {
        return targetDir;
    }

    public String getIgnorePatternFile() {
        return ignorePatternFile;
    }

    public List<String> getExtraIgnorePatterns() {
        return extraIgnorePatterns;
    }

    private AppPackageConfig(Builder builder) {
        this.basedir = builder.basedir;
        this.targetDir = builder.targetDir;
        this.ignorePatternFile = builder.ignorePatternFile;
        this.extraIgnorePatterns = builder.extraIgnorePatterns;
    }

    public static class Builder {

        private Folder basedir;
        private Folder targetDir;
        private String ignorePatternFile;
        private List<String> extraIgnorePatterns;

        public Builder() {
        }

        public Builder basedir(Folder basedir) {
            this.basedir = basedir;
            return this;
        }

        public Builder targetDir(Folder targetDir) {
            this.targetDir = targetDir;
            return this;
        }

        public Builder ignorePatternFile(String ignoreFile) {
            this.ignorePatternFile = ignoreFile;
            return this;
        }

        public Builder extraIgnorePatterns(List<String> extraIgnorePatterns) {
            this.extraIgnorePatterns = extraIgnorePatterns;
            return this;
        }

        public AppPackageConfig build() {
            ensureState();
            return new AppPackageConfig(this);
        }

        private void ensureState() {
            if (basedir == null || !basedir.exists()) {
                throw new IllegalStateException("BaseDir is mandatory to build AppPackageConfig");
            }

            if (targetDir == null || !targetDir.exists()) {
                throw new IllegalStateException("TargetDir is mandatory to build AppPackageConfig");
            }


        }
    }
}
