/*
 * Copyright 2019 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Assistant.createAssistant;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Proposals.applyToDocument;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.Proposals.proposalWithImage;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFileContent;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robotframework.ide.eclipse.main.plugin.mockdocument.Document;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourcePartitionScanner;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;

@ExtendWith(ProjectExtension.class)
public class CodeReservedWordsInSettingsAssistProcessorTest {

    @Project
    static IProject project;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        createFile(project, "suite.robot",
                "*** Settings ***",
                "Library  FooBar  ",
                "Resource  res.robot",
                "Library  FooBar  with xyz");
        createFile(project, "suite_settings.robot",
                "*** Settings ***",
                "Suite Setup  Some Keyword  arg",
                "Test Template  Not Defined",
                "Test Teardown  ",
                "Variables  file.py");
    }

    @Test
    public void codeReservedWordsProcessorIsValidOnlyForSettingsSection() {
        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        assertThat(processor.getApplicableContentTypes()).containsOnly(SuiteSourcePartitionScanner.SETTINGS_SECTION);
    }

    @Test
    public void codeReservedWordsProcessorHasTitleDefined() {
        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        assertThat(processor.getProposalsTitle()).isNotNull().isNotEmpty();
    }

    @Test
    public void noProposalsAreProvided_whenNotInApplicableContentType() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.KEYWORDS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 72);

        assertThat(proposals).isNull();
    }

    @Test
    public void libraryAliasProposalIsNotProvided_whenIsBeforeThirdCell() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 30);

        assertThat(proposals).isEmpty();
    }

    @Test
    public void libraryAliasProposalIsProvided_whenInApplicableContentType() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 72);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Library  FooBar  ", "Resource  res.robot",
                        "Library  FooBar  WITH NAME  alias"));
    }

    @Test
    public void libraryAliasProposalIsProvided_whenInApplicableContentTypeAndMatchesInput() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 74);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Library  FooBar  ", "Resource  res.robot",
                        "Library  FooBar  WITH NAME  alias"));
    }

    @Test
    public void libraryAliasProposalIsProvided_whenInApplicableContentTypeAndAtTheEndOfCell() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 34);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Library  FooBar  WITH NAME  alias",
                        "Resource  res.robot", "Library  FooBar  with xyz"));
    }

    @Test
    public void libraryAliasProposalIsNotProvided_whenInApplicableContentTypeButDoesNotMatchInput() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 80);

        assertThat(proposals).isEmpty();
    }

    @Test
    public void disableSettingProposalIsNotProvided_whenIsNotInSecondCell() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 44);

        assertThat(proposals).isEmpty();
    }

    @Test
    public void disableSettingProposalIsProvided_whenInApplicableContentType() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 63);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Suite Setup  Some Keyword  arg",
                        "Test Template  NONE", "Test Teardown  ", "Variables  file.py"));
    }

    @Test
    public void disableSettingProposalIsProvided_whenInApplicableContentTypeAndMatchesInput() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 65);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Suite Setup  Some Keyword  arg",
                        "Test Template  NONE", "Test Teardown  ", "Variables  file.py"));
    }

    @Test
    public void disableSettingProposalIsProvided_whenInApplicableContentTypeAndAtTheEndOfCell() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 90);

        assertThat(proposals).hasSize(1).are(proposalWithImage(null));

        assertThat(proposals).extracting(proposal -> applyToDocument(viewer.getDocument(), proposal))
                .containsOnly(new Document("*** Settings ***", "Suite Setup  Some Keyword  arg",
                        "Test Template  Not Defined", "Test Teardown  NONE", "Variables  file.py"));
    }

    @Test
    public void disableSettingProposalIsNotProvided_whenInApplicableContentTypeButDoesNotMatchInput() throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 40);

        assertThat(proposals).isEmpty();
    }

    @Test
    public void disableSettingProposalIsNotProvided_whenInApplicableContentTypeButNotInKeywordBasedSetting()
            throws Exception {
        final ITextViewer viewer = createViewer(getFile(project, "suite_settings.robot"),
                SuiteSourcePartitionScanner.SETTINGS_SECTION);

        final CodeReservedWordsInSettingsAssistProcessor processor = new CodeReservedWordsInSettingsAssistProcessor(
                createAssistant(getFile(project, "suite_settings.robot")));
        final List<? extends ICompletionProposal> proposals = processor.computeProposals(viewer, 102);

        assertThat(proposals).isEmpty();
    }

    private ITextViewer createViewer(final IFile file, final String contentType) throws BadLocationException {
        final ITextViewer viewer = mock(ITextViewer.class);
        final IDocument document = spy(new Document(getFileContent(file)));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getContentType(anyInt())).thenReturn(contentType);
        return viewer;
    }
}
