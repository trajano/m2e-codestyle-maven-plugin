package net.trajano.mojo.m2ecodestyle.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.junit.Rule;
import org.junit.Test;

import net.trajano.mojo.m2ecodestyle.FormatMojo;

public class FormatMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @SuppressWarnings("unchecked")
    @Test
    public void testFormatSingleFile() throws Exception {

        @SuppressWarnings("rawtypes")
        final Map options = DefaultCodeFormatterConstants.getJavaConventionsSettings();
        options.put(JavaCore.COMPILER_SOURCE, "1.7");
        options.put(JavaCore.COMPILER_COMPLIANCE, "1.7");
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
            "1.7");

        final CodeFormatter codeFormatter = new DefaultCodeFormatter(options);

        final File testPom = new File("src/test/resources/formatter/pom.xml");
        final FormatMojo mojo = (FormatMojo) rule.lookupConfiguredMojo(testPom.getParentFile(), "format");
        assertNotNull(mojo);

        final File temp = File.createTempFile("Temp", ".java");
        FileUtils.copyFile(new File("src/test/resources/BadlyFormatted.java"), temp);
        mojo.formatFile(temp, codeFormatter);
        System.out.println(temp);
        temp.delete();
    }

    @Test
    public void testFormatString() throws Exception {

        final CodeFormatter codeFormatter = new DefaultCodeFormatter(DefaultCodeFormatterConstants.getJavaConventionsSettings());
        final String content = "package x;import java.util.Date;class F { public int  a( Long x) { return Date.get();}}";
        final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, content, 0, content.length(), 0, null);

        DefaultCodeFormatter.DEBUG = true;
        System.out.println(((DefaultCodeFormatter) codeFormatter).getDebugOutput());

        final IDocument document = new Document();
        document.set(content);
        edit.apply(document);
        System.out.println(document.get());

    }
}