/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import org.artofsolving.jodconverter.office.OfficeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the LibreOfficeController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(LibreOfficeControllerTest.class)
public class LibreOfficeControllerTest extends AbstractTransformerControllerTest
{
    @SpyBean
    private LibreOfficeController controller;

    @Before
    public void before() throws IOException
    {
        super.controller = controller;

        sourceExtension = "doc";
        targetExtension = "pdf";
        sourceMimetype = "application/msword";

        // The following is based on super.mockTransformCommand(...)
        // This is because LibreOffice used JodConverter rather than a RuntimeExec

        expectedSourceFileBytes = Files.readAllBytes(getTestFile("quick."+sourceExtension, true).toPath());
        expectedTargetFileBytes = Files.readAllBytes(getTestFile("quick."+targetExtension, true).toPath());
        sourceFile = new MockMultipartFile("file", "quick."+sourceExtension, sourceMimetype, expectedSourceFileBytes);

        doAnswer((Answer) invocation ->
        {
            File sourceFile = invocation.getArgumentAt(0, File.class);
            File targetFile = invocation.getArgumentAt(1, File.class);

            assertNotNull(sourceFile);
            assertNotNull(targetFile);

            Long actualTimeout = invocation.getArgumentAt(2, Long.class);
            assertNotNull(actualTimeout);
            if (expectedTimeout != null)
            {
                assertEquals("expectedTimeout", expectedTimeout, actualTimeout);
            }

            // Copy a test file into the target file location if it exists
            String actualTarget = targetFile.getAbsolutePath();
            int i = actualTarget.lastIndexOf('_');
            if (i >= 0)
            {
                String testFilename = actualTarget.substring(i+1);
                File testFile = getTestFile(testFilename, false);
                if (testFile != null)
                {
                    FileChannel source = new FileInputStream(testFile).getChannel();
                    FileChannel target = new FileOutputStream(targetFile).getChannel();
                    target.transferFrom(source, 0, source.size());
                }
            }

            // Check the supplied source file has not been changed.
            byte[] actualSourceFileBytes = Files.readAllBytes(sourceFile.toPath());
            assertTrue("Source file is not the same", Arrays.equals(expectedSourceFileBytes, actualSourceFileBytes));

            return null;
        }).when(controller).convert(any(), any(), anyLong());
    }

    @Test
    @Override
    public void badExitCodeTest() throws Exception
    {
        doThrow(OfficeException.class).when(controller).convert(any(), any(), anyLong());

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", "xxx"))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("LibreOffice - LibreOffice server conversion failed:")));
    }
}