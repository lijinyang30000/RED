/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robotframework.ide.eclipse.main.plugin.assist.AssistProposalPredicates.alwaysTrue;
import static org.robotframework.ide.eclipse.main.plugin.assist.Commons.firstProposalContaining;
import static org.robotframework.ide.eclipse.main.plugin.assist.Commons.prefixesMatcher;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFile;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rf.ide.core.libraries.KeywordSpecification;
import org.rf.ide.core.libraries.LibraryDescriptor;
import org.rf.ide.core.libraries.LibrarySpecification;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.Libraries;
import org.robotframework.red.junit.jupiter.BooleanPreference;
import org.robotframework.red.junit.jupiter.PreferencesExtension;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;
import org.robotframework.red.junit.jupiter.StringPreference;

@ExtendWith({ ProjectExtension.class, PreferencesExtension.class })
public class RedKeywordProposalsTest {

    @Project
    static IProject project;

    private RobotModel robotModel;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        createFile(project, "res.robot",
                "*** Keywords ***",
                "a_res_kw1",
                "b_res_kw2",
                "c_res_kw3");
        createFile(project, "file_with_kw_conflict.robot",
                "*** Settings ***",
                "Library  LibA",
                "Library  LibB",
                "Library  LibC",
                "Library  LibD",
                "*** Test Cases ***");
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        robotModel = new RobotModel();
    }

    @Test
    public void noLocalKeywordsAreProvided_whenTheyDoNotMatchToGivenInputAndDefaultMatcherIsUsed()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw1",
                "b_kw2",
                "c_kw3",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        assertThat(provider.getKeywordProposals("other")).isEmpty();
    }

    @Test
    public void onlyLocalKeywordsMatchingGivenInputAreProvided_whenDefaultMatcherIsUsed() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw1",
                "b_kw2",
                "c_kw3",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("2");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("b_kw2 - file.robot");
    }

    @Test
    public void onlyLocalKeywordsMatchingGivenInputAreProvidedWithCorrectOrder_whenDefaultMatcherIsUsed()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw_ab",
                "b_kw_cd",
                "c_kw_ef",
                "ab_kw",
                "cd_kw",
                "ef_kw",
                "Enable Frequency",
                "Edit Configuration File",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("EF");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("Enable Frequency - file.robot", "ef_kw - file.robot", "c_kw_ef - file.robot");
    }

    @Test
    public void onlyLocalKeywordsMatchedByGivenMatcherAreProvided_whenProvidingCustomMatcher()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw1",
                "b_kw2",
                "c_kw3",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile, prefixesMatcher(),
                alwaysTrue());
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("c_");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("c_kw3 - file.robot");
    }

    @Test
    public void allLocalKeywordsAreProvided_whenTheyAreMatched() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw1",
                "b_kw2",
                "c_kw3",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_kw1 - file.robot", "b_kw2 - file.robot", "c_kw3 - file.robot");
    }

    @Test
    public void allLocalKeywordsAreProvidedInOrderInducedByGivenComparator_whenCustomComparatorIsProvided()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Keywords ***",
                "a_kw1",
                "b_kw2",
                "c_kw3",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final Comparator<? super RedKeywordProposal> comparator = firstProposalContaining("2");
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("", comparator);

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("b_kw2 - file.robot", "a_kw1 - file.robot", "c_kw3 - file.robot");
    }

    @Test
    public void noResourceKeywordsAreProvided_whenTheyDoNotMatchToGivenInputAndDefaultMatcherIsUsed()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        assertThat(provider.getKeywordProposals("other")).isEmpty();
    }

    @Test
    public void onlyResourceKeywordsMatchingGivenInputAreProvided_whenDefaultMatcherIsUsed() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("3");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("c_res_kw3 - res.robot");
    }

    @Test
    public void onlyResourceKeywordsMatchingGivenInputAreProvidedWithCorrectOrder_whenDefaultMatcherIsUsed()
            throws Exception {
        createFile(project, "res2.robot",
                "*** Keywords ***",
                "a_res_kw_ab",
                "b_res_kw_cd",
                "c_res_kw_ef",
                "ab_res_kw",
                "cd_res_kw",
                "ef_res_kw",
                "Create Duplicate",
                "Can Be Detected");
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res2.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("CD");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("Create Duplicate - res2.robot", "cd_res_kw - res2.robot", "b_res_kw_cd - res2.robot");
    }

    @Test
    public void onlyResourceKeywordsMatchedByGivenMatcherAreProvided_whenProvidingCustomMatcher() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile, prefixesMatcher(),
                AssistProposalPredicates.alwaysTrue());
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("b_");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("b_res_kw2 - res.robot");
    }

    @Test
    public void allResourceKeywordsAreProvided_whenTheyAreMatched() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_res_kw1 - res.robot", "b_res_kw2 - res.robot", "c_res_kw3 - res.robot");
    }

    @Test
    public void allResourceKeywordsAreProvidedInOrderInducedByGivenComparator_whenCustomComparatorIsProvided()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final Comparator<? super RedKeywordProposal> comparator = firstProposalContaining("2");
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("", comparator);

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("b_res_kw2 - res.robot", "a_res_kw1 - res.robot", "c_res_kw3 - res.robot");
    }

    @Test
    public void noLibraryKeywordsAreProvided_whenTheyDoNotMatchToGivenInputAndDefaultMatcherIsUsed()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw1", "b_slib_kw2", "c_slib_kw3"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1", "b_rlib_kw2", "c_rlib_kw3"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        assertThat(provider.getKeywordProposals("other")).isEmpty();
    }

    @Test
    public void onlyLibraryKeywordsMatchingGivenInputAreProvided_whenDefaultMatcherIsUsed() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw1", "b_slib_kw2", "c_slib_kw3"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1", "b_rlib_kw2", "c_rlib_kw3"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("2");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("b_rlib_kw2 - refLib", "b_slib_kw2 - stdLib");
    }

    @Test
    public void onlyLibraryKeywordsMatchingGivenInputAreProvidedWithCorrectOrder_whenDefaultMatcherIsUsed()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw_ab", "b_slib_kw_cd",
                "c_slib_kw_ef", "ab_slib_kw", "cd_slib_kw", "ef_slib_kw", "Add Bookmark", "Should Activate Block"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw_ab", "b_rlib_kw_cd",
                "c_rlib_kw_ef", "ab_rlib_kw", "cd_rlib_kw", "ef_rlib_kw", "Activate Bluetooth", "Get Active Block"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("AB");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("Activate Bluetooth - refLib", "Add Bookmark - stdLib", "ab_rlib_kw - refLib",
                        "ab_slib_kw - stdLib", "a_rlib_kw_ab - refLib", "a_slib_kw_ab - stdLib");
    }

    @Test
    public void onlyLibraryKeywordsMatchedByGivenMatcherAreProvided_whenProvidingCustomMatcher() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw1", "b_slib_kw2", "c_slib_kw3"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1", "b_rlib_kw2", "c_rlib_kw3"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile, prefixesMatcher(),
                AssistProposalPredicates.alwaysTrue());
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("c_");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("c_rlib_kw3 - refLib", "c_slib_kw3 - stdLib");
    }

    @Test
    public void allLibraryKeywordsAreProvided_whenTheyAreMatched() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw1", "b_slib_kw2", "c_slib_kw3"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1", "b_rlib_kw2", "c_rlib_kw3"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_rlib_kw1 - refLib", "a_slib_kw1 - stdLib", "b_rlib_kw2 - refLib",
                        "b_slib_kw2 - stdLib", "c_rlib_kw3 - refLib", "c_slib_kw3 - stdLib");
    }

    @Test
    public void allLibraryKeywordsAreProvidedInOrderInducedByGivenComparator_whenCustomComparatorIsProvided()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_kw", "b_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);
        final Comparator<? super RedKeywordProposal> comparator = firstProposalContaining("b_");
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("", comparator);

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("b_kw - stdLib", "a_kw - stdLib");
    }

    @Test
    public void onlyLibraryKeywordsFromLibrariesSatisfyingPredicateAreProvided_whenPredicateFiltersLibraries()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw1"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final AssistProposalPredicate<LibrarySpecification> predicate = libSpec -> !libSpec.getName().equals("refLib");
        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile, prefixesMatcher(),
                predicate);
        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("a_slib_kw1 - stdLib");
    }

    @Test
    public void allKeywordProposalsAreProvided_whenTheyArePrefixedWithBddSyntax() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_kw",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        for (final String bddPrefix : newArrayList("Given", "When", "And", "But", "Then")) {
            final List<? extends AssistProposal> proposals = provider.getKeywordProposals(bddPrefix + " a");

            assertThat(proposals).extracting(AssistProposal::getLabel)
                    .containsExactly("a_kw - file.robot", "a_res_kw1 - res.robot", "a_slib_kw - stdLib");
        }
    }

    @Test
    public void onlyKeywordProposalsMatchingQualifiedNameAreProvided_whenLibraryQualifiedNameIsUsed() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_kw",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("stdLib.");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("a_slib_kw - stdLib");
    }

    @Test
    public void onlyKeywordProposalsMatchingQualifiedNameAreProvided_whenLibraryQualifiedNameIsUsed_withAlias()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib  WITH NAME  myLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_kw",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        assertThat(provider.getKeywordProposals("stdLib.")).isEmpty();

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("myLib.");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("a_slib_kw - myLib");
    }

    @Test
    public void onlyKeywordProposalsMatchingQualifiedNameAreProvided_whenResourceQualifiedNameIsUsed()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_kw",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("res.");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_res_kw1 - res.robot", "b_res_kw2 - res.robot", "c_res_kw3 - res.robot");
    }

    @Test
    public void onlyKeywordProposalsMatchingQualifiedNameAreProvided_whenLocalQualifiedNameIsUsed() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_slib_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_kw",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("file.");

        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("a_kw - file.robot");
    }

    @Test
    public void keywordsUsingEmbeddedSyntaxAreProvided_whenPrefixMatchesArguments() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "*** Keywords ***",
                "kw with ${arg} and ${arg2}",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("kw with something");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("kw with ${arg} and ${arg2} - file.robot");
    }

    @StringPreference(key = RedPreferences.ASSISTANT_KEYWORD_PREFIX_AUTO_ADDITION, value = "ALWAYS")
    @Test
    public void qualifiedNameIsAddedToInputForProposals_whenKeywordPrefixAutoAdditionPreferenceIsSetToAlways()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_lib_kw1", "a_other_kw"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("a_");

        assertThat(proposals).extracting(AssistProposal::getContent)
                .containsExactly("stdLib.a_lib_kw1", "stdLib.a_other_kw");
    }

    @StringPreference(key = RedPreferences.ASSISTANT_KEYWORD_PREFIX_AUTO_ADDITION, value = "NEVER")
    @Test
    public void qualifiedNameIsNotAddedToInputForProposals_whenKeywordPrefixAutoAdditionPreferenceIsSetToNever()
            throws Exception {
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new LinkedHashMap<>();
        refLibs.putAll(Libraries.createRefLib("firstLib", "a_conflict_kw", "a_first_kw"));
        refLibs.putAll(Libraries.createRefLib("secondLib", "a_conflict_kw", "a_second_kw"));

        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setReferencedLibraries(refLibs);

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  firstLib",
                "Library  secondLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("a_");

        assertThat(proposals).extracting(AssistProposal::getContent)
                .containsExactly("a_conflict_kw", "a_conflict_kw", "a_first_kw", "a_second_kw");
    }

    @Test
    public void qualifiedNameIsAddedToInputForProposals_whenProposalFromNonLocalScopeIsConflicting() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_res_kw1",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("a_");

        assertThat(proposals).hasSize(3);

        for (final AssistProposal proposal : proposals) {
            final RedKeywordProposal keywordProposal = (RedKeywordProposal) proposal;

            switch (keywordProposal.getScope(file.getFullPath())) {
                case LOCAL:
                    assertThat(keywordProposal.getContent()).isEqualTo("a_res_kw1");
                    break;
                case RESOURCE:
                    assertThat(keywordProposal.getContent()).isEqualTo("res.a_res_kw1");
                    break;
                case STD_LIBRARY:
                    assertThat(keywordProposal.getContent()).isEqualTo("stdLib.a_res_kw1");
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Test
    public void onlyKeywordsFromImportedLibrariesOrAccessibleWithoutImportAreProvided_whenKeywordFromNotImportedLibraryPreferenceIsDisabled()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> stdLibs = new HashMap<>();
        stdLibs.putAll(Libraries.createStdLib("BuiltIn", "a_kw1"));
        stdLibs.putAll(Libraries.createStdLib("stdLib", "a_lib_kw1"));
        stdLibs.putAll(Libraries.createStdLib("otherLib", "a_other_kw1"));
        robotProject.setStandardLibraries(stdLibs);
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("a_");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_kw1 - BuiltIn", "a_lib_kw1 - stdLib");
    }

    @BooleanPreference(key = RedPreferences.ASSISTANT_KEYWORD_FROM_NOT_IMPORTED_LIBRARY_ENABLED, value = true)
    @Test
    public void allKeywordsFromProjectLibrariesAreProvided_whenKeywordFromNotImportedLibraryPreferenceIsEnabled()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> stdLibs = new HashMap<>();
        stdLibs.putAll(Libraries.createStdLib("BuiltIn", "a_kw1"));
        stdLibs.putAll(Libraries.createStdLib("stdLib", "a_lib_kw1"));
        stdLibs.putAll(Libraries.createStdLib("otherLib", "a_other_kw1"));
        robotProject.setStandardLibraries(stdLibs);
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("a_");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_kw1 - BuiltIn", "a_lib_kw1 - stdLib", "a_other_kw1 - otherLib",
                        "a_rlib_kw1 - refLib");
    }

    @BooleanPreference(key = RedPreferences.ASSISTANT_KEYWORD_FROM_NOT_IMPORTED_LIBRARY_ENABLED, value = true)
    @Test
    public void allKeywordsFromProjectLibrariesAreProvided_whenQualifiedNameIsUsedAndKeywordFromNotImportedLibraryPreferenceIsEnabled()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("otherLib", "a_other_kw1", "a_other_kw2"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_rlib_kw1"));

        final IFile file = createFile(project, "file.robot");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("otherLib.");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("a_other_kw1 - otherLib", "a_other_kw2 - otherLib");
    }

    @BooleanPreference(key = RedPreferences.ASSISTANT_KEYWORD_FROM_NOT_IMPORTED_LIBRARY_ENABLED, value = true)
    @Test
    public void allKeywordsFromProjectLibrariesAreProvidedButLibraryPrefixIsAddedOnlyWhenConflictExists_whenKeywordFromNotImportedLibraryPreferenceIsEnabled()
            throws Exception {
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new LinkedHashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibImported", "keyword"));
        refLibs.putAll(Libraries.createRefLib("LibNotImported", "keyword"));

        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setReferencedLibraries(refLibs);

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  LibImported",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final List<? extends AssistProposal> proposals = provider.getKeywordProposals("key");

        assertThat(proposals).extracting(AssistProposal::getLabel)
                .containsExactly("keyword - LibImported", "keyword - LibNotImported");
        assertThat(proposals).extracting(AssistProposal::getContent)
                .containsExactly("keyword", "LibNotImported.keyword");
    }

    @Test
    public void bestMatchingKeywordIsLocalWhenAllExist() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_res_kw1",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("a_res_kw1");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("file"));
    }

    @Test
    public void bestMatchingKeywordIsResourceKeywordIfThereIsNoLocal() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "Resource  res.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("a_res_kw1");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("res"));
    }

    @Test
    public void bestMatchingKeywordIsUserLibKeywordIfThereIsNoLocalAndResource() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("a_res_kw1");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("refLib"));
    }

    @Test
    public void bestMatchingKeywordIsStdLibKeywordIfThereAreNoOther() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("a_res_kw1");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("stdLib"));
    }

    @Test
    public void bestMatchingKeywordIsNullForUnknownName() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(Libraries.createStdLib("stdLib", "a_res_kw1"));
        robotProject.setReferencedLibraries(Libraries.createRefLib("refLib", "a_res_kw1"));

        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  stdLib",
                "Library  refLib",
                "Resource  res.robot",
                "*** Keywords ***",
                "a_res_kw1",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("unknown");
        assertThat(bestMatch).isNotPresent();
    }

    @Test
    public void bestMatchingKeywordIsFirstFoundInScopeWhenConflictExists() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b", "c")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibA"));
    }

    @StringPreference(key = RedPreferences.ASSISTANT_MATCHING_KEYWORD, value = "MIN_REQUIRED_ARGS")
    @Test
    public void bestMatchingKeywordIsKeywordWithMinimumRequiredArgumentsWhenConflictExists() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b", "c")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibC"));
    }

    @StringPreference(key = RedPreferences.ASSISTANT_MATCHING_KEYWORD, value = "MAX_REQUIRED_ARGS")
    @Test
    public void bestMatchingKeywordIsKeywordWithMaximumRequiredArgumentsWhenConflictExists() throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b", "c")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibB"));
    }

    @Test
    public void bestMatchingKeywordIsFirstFoundInScopeWhenConflictExistsAndRequiredArgumentSizeIsEqual()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b=1", "c=2")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b=1")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a", "b=1", "*args")));
        refLibs.putAll(Libraries.createRefLib("LibD", KeywordSpecification.create("Keyword", "a", "**kwargs")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibA"));
    }

    @StringPreference(key = RedPreferences.ASSISTANT_MATCHING_KEYWORD, value = "MIN_REQUIRED_ARGS")
    @Test
    public void bestMatchingKeywordIsKeywordWithMinimumOptionalArgumentsWhenConflictExistsAndRequiredArgumentSizeIsEqual()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b=1", "c=2")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b=1")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a", "b=1", "*args")));
        refLibs.putAll(Libraries.createRefLib("LibD", KeywordSpecification.create("Keyword", "a", "**kwargs")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibB"));
    }

    @StringPreference(key = RedPreferences.ASSISTANT_MATCHING_KEYWORD, value = "MAX_REQUIRED_ARGS")
    @Test
    public void bestMatchingKeywordIsKeywordWithMaximumOptionalArgumentsWhenConflictExistsAndRequiredArgumentSizeIsEqual()
            throws Exception {
        final RobotProject robotProject = robotModel.createRobotProject(project);
        final Map<LibraryDescriptor, LibrarySpecification> refLibs = new HashMap<>();
        refLibs.putAll(Libraries.createRefLib("LibA", KeywordSpecification.create("Keyword", "a", "b=1", "c=2")));
        refLibs.putAll(Libraries.createRefLib("LibB", KeywordSpecification.create("Keyword", "a", "b=1")));
        refLibs.putAll(Libraries.createRefLib("LibC", KeywordSpecification.create("Keyword", "a", "b=1", "*args")));
        refLibs.putAll(Libraries.createRefLib("LibD", KeywordSpecification.create("Keyword", "a", "**kwargs")));
        robotProject.setReferencedLibraries(refLibs);

        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(getFile(project, "file_with_kw_conflict.robot"));

        final RedKeywordProposals provider = new RedKeywordProposals(robotModel, suiteFile);

        final Optional<RedKeywordProposal> bestMatch = provider.getBestMatchingKeywordProposal("Keyword");
        assertThat(bestMatch).hasValueSatisfying(proposal -> assertThat(proposal.getSourceName()).isEqualTo("LibC"));
    }
}
