package net.darkhax.parabox.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;

import net.darkhax.parabox.util.ZipUtils.MyZipFile;

public class BlacklistedFileUtils {

	public static final List<String> IGNORED = new ArrayList<>();

	public static void delete(File worldDir) throws IOException {
		for (File f : worldDir.listFiles(filter)) {
			if (f.isDirectory()) FileUtils.deleteDirectory(f);
			else f.delete();
		}
	}

	public static FileFilter filter = f -> !IGNORED.contains(f.getName());
	
    public static void unzipFolder (File File, File zipDestinationFolder) {

        try (MyZipFile zipFile = new MyZipFile(File)) {

            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();

                String name = zipEntry.getName();
                if(isBlacklisted(name)) continue;
                if (zipEntry.isDirectory()) {
                    if (name.endsWith("/") || name.endsWith("\\")) {
                        name = name.substring(0, name.length() - 1);
                    }

                    final File newDir = new File(zipDestinationFolder, name);
                    if (!newDir.exists() && !newDir.mkdirs()) {
                        throw new RuntimeException("Creation of target dir was failed, target dir: " + zipDestinationFolder + ", entity: " + name);
                    }
                }
                else {
                    final File destinationFile = ZipUtils.createTargetFile(zipDestinationFolder, name);
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
    
    public static boolean isBlacklisted(String path) {
    	for(String s : IGNORED) 
    		if (path.contains(s)) return true;
    	return false;
    }
}
