package net.trajano.mojo.m2ecodestyle.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;

import javax.inject.Inject;

import org.codehaus.plexus.util.io.URLInputStreamFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import net.trajano.mojo.m2ecodestyle.Retrieval;

/**
 * Default implementation of {@link Retrieval}
 */
public class DefaultRetrieval implements
    Retrieval {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRetrieval.class);

    /**
     * Build context.
     */
    @Inject
    private BuildContext buildContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchAndMerge(final URI codeStyleBaseUri,
        final String prefsFile,
        final File destDir) throws IOException {

        final File destFile = new File(destDir, prefsFile);
        final Properties props = new Properties();
        if (destFile.exists()) {
            final FileInputStream fileInputStream = new FileInputStream(destFile);
            props.load(fileInputStream);
            fileInputStream.close();
        }

        final InputStream prefsInputStream = openPreferenceStream(codeStyleBaseUri, prefsFile);

        if (prefsInputStream == null) {
            return;
        }
        props.load(prefsInputStream);
        prefsInputStream.close();

        final OutputStream outputStream = buildContext.newFileOutputStream(destFile);
        props.store(outputStream, "Generated by m2e codestyle maven plugin");
        outputStream.close();
    }

    /**
     * Performs the actual work of getting the stream.
     *
     * @param resolved
     *            resolved URI
     * @return stream or <code>null</code> if the target is not available.
     * @throws IOException
     *             I/O error
     */
    private InputStream internalOpenStream(final URI resolved) throws IOException {

        try {
            if (resolved.isAbsolute()) {
                return new URLInputStreamFacade(resolved.toURL()).getInputStream();
            } else {
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(resolved.toString());
            }
        } catch (final FileNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openPreferenceStream(final URI codeStyleBaseUri,
        final String prefsFile) throws IOException {

        final URI resolved = codeStyleBaseUri.resolve(prefsFile);
        return internalOpenStream(resolved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream(final String url) throws IOException {

        final URI resolved = URI.create(url);
        return internalOpenStream(resolved);
    }
}
