/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.app.build;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.File;
import com.wavemaker.commons.io.FilterOn;
import com.wavemaker.commons.io.Folder;

/**
 * Created by saddhamp on 24/4/16.
 */
public class BasePackage {

    private Folder sourceFolder;
    private String basePackageName;

    public BasePackage(Folder sourceFolder) {
        if(sourceFolder == null || !sourceFolder.exists())
            throw new WMRuntimeException("Source folder is null or does not exist");

        this.sourceFolder = sourceFolder;
        computeBasePackage();
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    /**
     * Given source folder, computes the base package
     *
     * -Note:
     *  Non-empty path - length of path (excluding the java file name
     *  i.e path from source folder to its parent folder) from source folder > 0
     *
     * -Algorithm working:
     *  1. Find all java files
     *  2. If empty path does not exist
     *      1. Initialize longest common path & length of longest common path
     *      2. Iterates over the rest of the paths, update length of longest common path
     *          during each iteration if necessary
     *      3. Finally, if longest common path length is greater than zero,
     *          extract longest common path and convert it to package notation
     *
     * -Samples:
     *  1. Paths: com, cim >> Output: null
     *  2. Paths: com, com/wavemaker >> Output: com
     *  3. Paths: "", "" >> Output: null
     *  4. Paths: com/wave, com/wavemaker >> Output: com
     *  5. Paths: com/wavemaker, com/wavemaker >> Output: com.wavemaker
     *  6. Paths: org/wavemaker, com/wavemaker >> Output: null
     *
     */
    private void computeBasePackage() {
        List<File> javaFiles = sourceFolder.find().files().include(FilterOn.names().ending(".java")).fetchAll();
        if (javaFiles.size() > 0){
            boolean emptyPathExists = checkIfEmptyPathExists(javaFiles);

            if(!emptyPathExists) {
                String longestCommonPath = javaFiles.get(0).getParent().toStringRelativeTo(sourceFolder);
                int longestCommonPathLength = longestCommonPath.length();

                for (int i = 1; i < javaFiles.size(); i++) {
                    String currentPath = javaFiles.get(i).getParent().toStringRelativeTo(sourceFolder);
                    //Update longestCommonPathLength if common path between currentPath and longestCommonPathLength is less than longestCommonPathLength
                    longestCommonPathLength = getNewCommonPathLength(longestCommonPath, longestCommonPathLength, currentPath);
                }

                if(longestCommonPathLength>0){
                    //Extracting the longest common package
                    longestCommonPath = longestCommonPath.substring(0, longestCommonPathLength);
                    //Convert to package notation
                    basePackageName = longestCommonPath.replace("/", ".");
                }
            }
        }
    }

    private boolean checkIfEmptyPathExists(List<File> files){
        boolean emptyPathExists = false;

        if (files.size() > 0){
            for (File file : files) {
                //Check if current folder of file is same as source folder
                if (StringUtils.isBlank(file.getParent().toStringRelativeTo(sourceFolder))) {
                    emptyPathExists = true;
                    break;
                }
            }
        }

        return emptyPathExists;
    }

    private int getNewCommonPathLength(String longestCommonPath, int longestCommonPathLength, String nextPath) {

        int minLength = Math.min(longestCommonPathLength, nextPath.length());
        int newLongstComnPathLen = 0;
        int potentialLongstComnPathLen = 0;

        for(int i=0; i<minLength; i++){
            //Break if current character doesn't
            if(longestCommonPath.charAt(i)!=nextPath.charAt(i))
                break;

            //Potential longest common path length, used when whole folder name or path name matches
            potentialLongstComnPathLen++;

            //Update only when whole folder name or path name matches
            if(longestCommonPath.charAt(i)=='/' || i==(minLength-1)) {
                newLongstComnPathLen = potentialLongstComnPathLen;
            }
        }

        //Ignore last delimiter after folder name
        if(longestCommonPath.charAt(newLongstComnPathLen-1)=='/'){
            newLongstComnPathLen = newLongstComnPathLen-1;
        }

        return newLongstComnPathLen;
    }
}
