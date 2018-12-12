package org.alfresco.transformer.fs;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transformer.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.util.TempFileProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

/**
 */
public class FileManager
{
    public static final String SOURCE_FILE = "sourceFile";
    public static final String TARGET_FILE = "targetFile";
    private static final String FILENAME = "filename=";

    /**
     * Returns a File to be used to store the result of a transformation.
     *
     * @param request
     * @param filename The targetFilename supplied in the request. Only the filename if a path is used as part of the
     *                 temporary filename.
     * @return a temporary File.
     * @throws TransformException if there was no target filename.
     */
    public static File createTargetFile(HttpServletRequest request, String filename)
    {
        File file = buildFile(filename);
        request.setAttribute(TARGET_FILE, file);
        return file;
    }

    public static File buildFile(String filename)
    {
        filename = checkFilename( false, filename);
        LogEntry.setTarget(filename);
        return TempFileProvider.createTempFile("target_", "_" + filename);
    }

    public static void deleteFile(final File file) throws Exception
    {
        if (!file.delete())
        {
            throw new Exception("Failed to delete file");
        }
    }

    /**
     * Checks the filename is okay to uses in a temporary file name.
     *
     * @param filename or path to be checked.
     * @return the filename part of the supplied filename if it was a path.
     * @throws TransformException if there was no target filename.
     */
    private static String checkFilename(boolean source, String filename)
    {
        filename = StringUtils.getFilename(filename);
        if (filename == null || filename.isEmpty())
        {
            String sourceOrTarget = source ? "source" : "target";
            int statusCode = source ? BAD_REQUEST.value() : INTERNAL_SERVER_ERROR.value();
            throw new TransformException(statusCode, "The " + sourceOrTarget + " filename was not supplied");
        }
        return filename;
    }

    private static void save(MultipartFile multipartFile, File file)
    {
        try
        {
            Files.copy(multipartFile.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE.value(),
                "Failed to store the source file", e);
        }
    }

    public static void save(Resource body, File file)
    {
        try
        {
            Files.copy(body.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new TransformException(INSUFFICIENT_STORAGE.value(), "Failed to store the source file", e);
        }
    }

    private static Resource load(File file)
    {
        try
        {
            Resource resource = new UrlResource(file.toURI());
            if (resource.exists() || resource.isReadable())
            {
                return resource;
            }
            else
            {
                throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Could not read the target file: " + file.getPath());
            }
        }
        catch (MalformedURLException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "The target filename was malformed: " + file.getPath(), e);
        }
    }

    public static String getFilenameFromContentDisposition(HttpHeaders headers)
    {
        String filename = "";
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition != null)
        {
            String[] strings = contentDisposition.split("; *");
            filename = Arrays.stream(strings)
                             .filter(s -> s.startsWith(FILENAME))
                             .findFirst()
                             .map(s -> s.substring(FILENAME.length()))
                             .orElse("");
        }
        return filename;
    }


    /**
     * Returns the file name for the target file
     *
     * @param fileName Desired file name
     * @param targetExtension File extension
     * @return Target file name
     */
    public static String createTargetFileName(String fileName, String targetExtension)
    {
        String targetFilename = null;
        String sourceFilename = fileName;
        sourceFilename = StringUtils.getFilename(sourceFilename);
        if (sourceFilename != null && !sourceFilename.isEmpty())
        {
            String ext = StringUtils.getFilenameExtension(sourceFilename);
            targetFilename = (ext != null && !ext.isEmpty()
                              ? sourceFilename.substring(0, sourceFilename.length()-ext.length()-1)
                              : sourceFilename)+
                             '.'+targetExtension;
        }
        return targetFilename;
    }

    /**
     * Returns a File that holds the source content for a transformation.
     *
     * @param request
     * @param multipartFile from the request
     * @return a temporary File.
     * @throws TransformException if there was no source filename.
     */
    public static File createSourceFile(HttpServletRequest request, MultipartFile multipartFile)
    {
        String filename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();
        filename = checkFilename(  true, filename);
        File file = TempFileProvider.createTempFile("source_", "_" + filename);
        request.setAttribute(SOURCE_FILE, file);
        save(multipartFile, file);
        LogEntry.setSource(filename, size);
        return file;
    }

    public static void deleteFile(HttpServletRequest request, String attributeName)
    {
        File file = (File) request.getAttribute(attributeName);
        if (file != null)
        {
            file.delete();
        }
    }

    public static ResponseEntity<Resource> createAttachment(String targetFilename, File
        targetFile)
    {
        Resource targetResource = load(targetFile);
        targetFilename = UriUtils.encodePath(StringUtils.getFilename(targetFilename), "UTF-8");
        return  ResponseEntity.ok().header(HttpHeaders
                .CONTENT_DISPOSITION,
            "attachment; filename*= UTF-8''" + targetFilename).body(targetResource);
    }
}