/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.variables.mapping;

import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.model.table.VariableTable;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver.PositionExpected;
import org.robotframework.ide.core.testData.model.table.mapping.IParsingMapper;
import org.robotframework.ide.core.testData.model.table.variables.AVariable.VariableScope;
import org.robotframework.ide.core.testData.model.table.variables.ScalarVariable;
import org.robotframework.ide.core.testData.text.read.IRobotTokenType;
import org.robotframework.ide.core.testData.text.read.ParsingState;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;


public class ScalarVariableMapper implements IParsingMapper {

    private final ElementPositionResolver positionResolver;
    private final CommonVariableHelper varHelper;


    public ScalarVariableMapper() {
        this.positionResolver = new ElementPositionResolver();
        this.varHelper = new CommonVariableHelper();
    }


    @Override
    public RobotToken map(final RobotLine currentLine,
            final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final RobotToken rt, final FilePosition fp,
            final String text) {
        final VariableTable varTable = robotFileOutput.getFileModel()
                .getVariableTable();
        rt.setText(text);
        rt.setRaw(text);
        rt.setType(RobotTokenType.VARIABLES_SCALAR_DECLARATION);

        final ScalarVariable var = new ScalarVariable(
                varHelper.extractVariableName(text), rt,
                VariableScope.TEST_SUITE);
        varTable.addVariable(var);

        processingState.push(ParsingState.SCALAR_VARIABLE_DECLARATION);

        return rt;
    }


    @Override
    public boolean checkIfCanBeMapped(final RobotFileOutput robotFileOutput,
            final RobotLine currentLine, final RobotToken rt, final String text,
            final Stack<ParsingState> processingState) {
        boolean result = false;
        final List<IRobotTokenType> types = rt.getTypes();
        if (types.size() == 1
                && types.get(0) == RobotTokenType.VARIABLES_SCALAR_DECLARATION) {
            if (positionResolver.isCorrectPosition(
                    PositionExpected.VARIABLE_DECLARATION_IN_VARIABLE_TABLE,
                    robotFileOutput.getFileModel(), currentLine, rt)) {
                if (varHelper.isIncludedInVariableTable(currentLine,
                        processingState)) {
                    if (varHelper.isCorrectVariable(text)) {
                        result = true;
                    } else {
                        // FIXME: error here or in validation
                    }
                } else {
                    // FIXME: it is in wrong place means no variable table
                    // declaration
                }
            } else {
                // FIXME: wrong place case.
            }
        }
        return result;
    }
}
