/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer.transformers;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * The SelectingTransformer selects a registered {@link SelectableTransformer}
 * and delegates the transformation to its implementation.
 *
 * @author eknizat
 */
@Component
public class SelectingTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(SelectingTransformer.class);

    private final List<SelectableTransformer> transformers;

    public SelectingTransformer()
    {
        transformers = ImmutableList.of(
            new AppleIWorksContentTransformer(),
            new HtmlParserContentTransformer(),
            new StringExtractingContentTransformer(),
            new TextToPdfContentTransformer()
            // new OOXMLThumbnailContentTransformer()); // Doesn't work with java 11, transformer and test disabled
        );
    }

    /**
     * Performs a transform using a transformer selected based on the provided sourceMimetype and targetMimetype
     *
     * @param sourceFile     File to transform from
     * @param targetFile     File to transform to
     * @param sourceMimetype Mimetype of the source file
     * @throws TransformException
     */
    public void transform(File sourceFile, File targetFile, String sourceMimetype,
        String targetMimetype, Map<String, String> parameters) throws TransformException
    {
        try
        {
            final SelectableTransformer transformer = selectTransformer(sourceMimetype,
                targetMimetype, parameters);
            logOptions(sourceFile, targetFile, parameters);
            transformer.transform(sourceFile, targetFile, parameters);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), getMessage(e));
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
        }
        if (!targetFile.exists())
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file. Target file does not exist.");
        }
        if (sourceFile.length() > 0 && targetFile.length() == 0)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file. Target file is empty but source file was not empty.");
        }
    }

    private SelectableTransformer selectTransformer(String sourceMimetype, String targetMimetype,
        Map<String, String> parameters)
    {
        for (SelectableTransformer transformer : transformers)
        {
            if (transformer.isTransformable(sourceMimetype, targetMimetype, parameters))
            {
                logger.debug("Using {} to transform from {} to {}",
                    transformer.getClass().getName(), sourceMimetype, targetMimetype);
                return transformer;
            }
        }
        throw new RuntimeException(
            "Could not select a transformer for sourceMimetype=" + sourceMimetype
            + " targetMimetype=" + targetMimetype);
    }

    private static String getMessage(Exception e)
    {
        return e.getMessage() == null || e.getMessage().isEmpty() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private void logOptions(File sourceFile, File targetFile, Map<String, String> parameters)
    {
        StringJoiner sj = new StringJoiner(" ");
        parameters.forEach((k, v) -> sj.add(
            "--" + k + "=" + v)); // keeping the existing style used in other T-Engines
        sj.add(getExtension(sourceFile));
        sj.add(getExtension(targetFile));
        LogEntry.setOptions(sj.toString());
    }

    private String getExtension(File file)
    {
        final String name = file.getName();
        int i = name.lastIndexOf('.');
        return i == -1 ? "???" : name.substring(i + 1);
    }
}
