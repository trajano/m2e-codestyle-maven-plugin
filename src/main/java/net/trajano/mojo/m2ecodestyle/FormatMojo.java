package net.trajano.mojo.m2ecodestyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.FileSet;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "format",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true,
    requiresOnline = false)
public class FormatMojo extends AbstractMojo {

    /**
     * Build context.
     */
    @Component
    private BuildContext buildContext;

    /**
     * <p>
     * This is the URL that points to the base URL where the files are located.
     * </p>
     * <p>
     * The URL <b>must</b> end with a trailing slash as the names referenced by
     * {@link #prefsFiles} are resolved against it. If the trailing slash is
     * missing, it will append it automatically and log a warning.
     * </p>
     * <p>
     * If this is not an absolute URL, it assumes that the value passed in is
     * referring to something in the classpath.
     * </p>
     * <p>
     * If the value is not specified, then the default Java conventions would be
     * used.
     * </p>
     */
    @Parameter(required = false)
    private String codeStyleBaseUrl;

    /**
     * The Maven Project.
     */
    @Parameter(defaultValue = "${project}",
        readonly = true)
    private MavenProject project;

    /**
     * Injected property retrieval component.
     */
    @Component
    private PropertyRetrieval retrieval;

    @Parameter(property = "maven.compiler.source",
        defaultValue = "1.5")
    private String source;

    @Parameter(property = "maven.compiler.target",
        defaultValue = "1.5")
    private String target;

    @SuppressWarnings("unchecked")
    public void addJavaCoreProperties(@SuppressWarnings("rawtypes") final Map options) {

        final Plugin plugin = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        if (plugin == null) {
            getLog().warn("Maven compiler plugin is not present, will use the default Java targets");
        }
        options.put(JavaCore.COMPILER_SOURCE, source);
        options.put(JavaCore.COMPILER_COMPLIANCE, source);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
            target);

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            final CodeFormatter codeFormatter;
            if (codeStyleBaseUrl == null) {
                final Map<?, ?> options = DefaultCodeFormatterConstants.getJavaConventionsSettings();
                addJavaCoreProperties(options);
                codeFormatter = ToolFactory.createCodeFormatter(options);
            } else {
                final URI codeStyleBaseUri = new URI(codeStyleBaseUrl);
                final Properties props = new Properties();

                final InputStream prefStream = retrieval.openPreferenceStream(codeStyleBaseUri, "org.eclipse.jdt.core.prefs");
                if (prefStream == null) {
                    throw new MojoExecutionException("unable to retrieve org.eclipse.jdt.core.prefs from " + codeStyleBaseUri);
                }
                props.load(prefStream);
                prefStream.close();
                addJavaCoreProperties(props);
                codeFormatter = ToolFactory.createCodeFormatter(props);
            }
            final FileSet sourceSet = new FileSet();
            sourceSet.setDirectory(project.getBuild().getSourceDirectory());
            sourceSet.addInclude("**/*.java");

            final FileSet testSet = new FileSet();
            testSet.setDirectory(project.getBuild().getTestSourceDirectory());
            testSet.addInclude("**/*.java");

            for (final FileSet xmlFiles : new FileSet[] { sourceSet,
                testSet }) {
                final File dir = new File(xmlFiles.getDirectory());
                if (!dir.exists()) {
                    continue;
                }
                final org.codehaus.plexus.util.Scanner scanner = buildContext.newScanner(dir, false);
                scanner.setIncludes(xmlFiles.getIncludes().toArray(new String[0]));
                scanner.scan();
                for (final String includedFile : scanner.getIncludedFiles()) {
                    final File file = new File(scanner.getBasedir(), includedFile);
                    formatFile(file, codeFormatter);
                }
            }

        } catch (final MalformedURLException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (final URISyntaxException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Formats an individual file. Made public to allow testing.
     *
     * @param file
     * @param codeFormatter
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void formatFile(final File file,
        final CodeFormatter codeFormatter) throws MojoExecutionException, MojoFailureException {

        final IDocument doc = new Document();
        try {
            //            final Scanner scanner = new Scanner(file);
            //          final String content = scanner.useDelimiter("\\Z").next();

            final String content = new String(org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(file, null));

            doc.set(content);
            //            scanner.close();

            final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, content, 0, content.length(), 0, null);

            if (edit != null) {
                edit.apply(doc);
            } else {
                DefaultCodeFormatter.DEBUG = true;
                System.out.println(((DefaultCodeFormatter) codeFormatter).getDebugOutput());
                throw new MojoFailureException("unable to format " + file);
            }

            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(buildContext.newFileOutputStream(file)));
            try {

                out.write(doc.get());
                out.flush();

            } finally {

                try {
                    out.close();
                } catch (final IOException e) {
                }

            }

        } catch (final IOException e) {
            throw new MojoExecutionException("IO Exception" + file, e);
        } catch (final BadLocationException e) {
            throw new MojoFailureException("Bad Location Exception " + file, e);
        }
    }
}