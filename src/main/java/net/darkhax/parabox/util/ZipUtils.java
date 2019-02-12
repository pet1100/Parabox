/*
 AiAi, Copyright (C) 2017 - 2018, Serge Maslyukov

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */
package net.darkhax.parabox.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Utility to Zip and Unzip nested directories recursively. Author of 1st version is:
 *
 * @author Robin Spark circa 13.07.12
 *
 */
public class ZipUtils {
    /*
     * @param directoryPath The path of the directory where the  will be created. eg.
     * c:/temp
     * @param zipFile The full path of the  to create. eg. c:/temp/.zip
     */
    public static void createZip (String directoryPath, String zipPath) throws IOException {

        createZip(new File(directoryPath), new File(zipPath));
    }

    /**
     * Creates a zip file at the specified path with the contents of the specified directory.
     * NB:
     *
     * @param directory The path of the directory where the  will be created. eg.
     *        c:/temp
     * @param zipFile zip file
     * @throws IOException If anything goes wrong
     */
    public static void createZip (File directory, File zipFile) throws IOException {

        try (FileOutputStream fOut = new FileOutputStream(zipFile); BufferedOutputStream bOut = new BufferedOutputStream(fOut); ZipOutputStream tOut = new ZipOutputStream(bOut)) {
            addFileToZip(tOut, directory, "");
        }
    }

    /**
     * Creates a zip entry for the path specified with a name built from the base passed in and
     * the file/directory name. If the path is a directory, a recursive call is made such that
     * the full directory is added to the zip.
     *
     * @param zOut The zip file's output stream
     * @param f The filesystem path of the file/directory being added
     * @param base The base prefix to for the name of the zip file entry
     *
     * @throws IOException If anything goes wrong
     */
    private static void addFileToZip (ZipOutputStream zOut, File f, String base) throws IOException {

        String entryName = base + f.getName();
        entryName = f.isDirectory() && !entryName.endsWith("/") ? entryName + "/" : entryName;
        final ZipEntry zipEntry = new ZipEntry(entryName);
           if (f.isFile()){
               zipEntry.setSize(f.length());
           }
           zipEntry.setTime(f.lastModified());

        zOut.putNextEntry(zipEntry);

        if (f.isFile()) {
            try (FileInputStream fInputStream = new FileInputStream(f)) {
                IOUtils.copy(fInputStream, zOut);
                zOut.closeEntry();
            }
        }
        else {
            zOut.closeEntry();
            final File[] children = f.listFiles();

            if (children != null) {
                for (final File child : children) {
                    addFileToZip(zOut, child.getAbsoluteFile(), entryName + "/");
                }
            }
        }
    }

    /**
     * Extract zip file at the specified destination path. NB: must consist of a single
     * root folder containing everything else
     *
     * @param Path path to zip file
     * @param destinationPath path to extract zip file to. Created if it doesn't exist.
     */
    public static void extractZip (String Path, String destinationPath) {

        final File File = new File(Path);
        File unzipDestFolder;
        try {
            unzipDestFolder = new File(destinationPath);
            unzipFolder(File, unzipDestFolder);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyZipFile extends ZipFile implements Cloneable, AutoCloseable {

        public MyZipFile (File f) throws IOException {

            super(f);
        }
    }

    /**
     * Unzips a zip file into the given destination directory.
     *
     * The  file MUST have a unique "root" folder. This root folder is skipped when
     * unarchiving.
     *
     */
    public static void unzipFolder (File File, File zipDestinationFolder) {

        try (MyZipFile zipFile = new MyZipFile(File)) {

            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();

                String name = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    if (name.endsWith("/") || name.endsWith("\\")) {
                        name = name.substring(0, name.length() - 1);
                    }

                    final File newDir = new File(zipDestinationFolder, name);
                    if (!newDir.mkdirs()) {
                        throw new RuntimeException("Creation of target dir was failed, target dir: " + zipDestinationFolder + ", entity: " + name);
                    }
                }
                else {
                    final File destinationFile = createTargetFile(zipDestinationFolder, name);
                    if (destinationFile == null) {
                        throw new RuntimeException("Creation of target file was failed, target dir: " + zipDestinationFolder + ", entity: " + name);
                    }
                    if (!destinationFile.getParentFile().exists()) {
                        destinationFile.getParentFile().mkdirs();
                    }
                    FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), destinationFile);
                }
            }
        }
        catch (final IOException e) {
            throw new RuntimeException("Unzip failed:", e);
        }
    }

    public static File createTargetFile (File zipDestinationFolder, String name) {

        final File destinationFile = new File(zipDestinationFolder, name);
        if (name.endsWith(File.separator)) {
            if (!destinationFile.isDirectory() && !destinationFile.mkdirs()) {
                throw new RuntimeException("Error creating temp directory:" + destinationFile.getPath());
            }
            return destinationFile;
        }
        else if (name.indexOf(File.separatorChar) != -1) {
            // Create the the parent directory if it doesn't exist
            final File parentFolder = destinationFile.getParentFile();
            if (!parentFolder.isDirectory()) {
                if (!parentFolder.mkdirs()) {
                    throw new RuntimeException("Error creating temp directory:" + parentFolder.getPath());
                }
            }
        }
        return destinationFile;
    }
}