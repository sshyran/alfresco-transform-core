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

import java.io.File;
import java.util.Map;

/**
 * Implemented by transformers used by {@link SelectingTransformer}.
 *
 * @author eknizat
 */
public interface SelectableTransformer
{
    String SOURCE_ENCODING = "sourceEncoding";
    String TARGET_ENCODING = "targetEncoding";
    String SOURCE_MIMETYPE = "sourceMimetype";
    String TARGET_MIMETYPE = "targetMimetype";

    /**
     * Implementation of the actual transformation.
     *
     * @param sourceFile
     * @param targetFile
     * @param parameters
     * @throws Exception
     */
    void transform(File sourceFile, File targetFile, Map<String, String> parameters)
        throws Exception;

    /**
     * Determine whether this transformer is applicable for the given MIME types.
     *
     * @param sourceMimetype
     * @param targetMimetype
     * @param parameters
     * @return
     */
    boolean isTransformable(String sourceMimetype, String targetMimetype,
        Map<String, String> parameters);
}
