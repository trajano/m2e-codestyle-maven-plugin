package net.trajano.mojo.m2ecodestyle.test;

import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import net.trajano.mojo.m2ecodestyle.internal.DefaultRetrieval;

public class RetrievalTest {

    @Test
    public void testFileNotFound() throws IOException {

        assertNull(new DefaultRetrieval().openStream("nowhere"));
    }
}
