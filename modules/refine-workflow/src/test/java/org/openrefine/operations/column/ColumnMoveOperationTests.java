/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.operations.column;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnMoveOperationTests extends RefineTest {

    protected Grid initialState;

    @BeforeMethod
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        initialState = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
    }

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation("core", "column-move", ColumnMoveOperation.class);
    }

    @Test
    public void serializeColumnMoveOperationWithIndex() throws Exception {
        String json = "{\"op\":\"core/column-move\","
                + "\"description\":\"Move column \\\"my column\\\" to position 3\","
                + "\"columnName\":\"my column\","
                + "\"index\":3}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnMoveOperation.class), json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeColumnMoveOperationWithAfterColumn() throws Exception {
        String json = "{\"op\":\"core/column-move\","
                + "\"description\":\"Move column \\\"my column\\\" to after column \\\"my other column\\\"\","
                + "\"columnName\":\"my column\","
                + "\"afterColumn\":\"my other column\","
                + "\"columnDeletions\" : [ \"my column\" ],"
                + "\"columnInsertions\" : [ {"
                + "  \"copiedFrom\" : \"my column\","
                + "  \"insertAt\" : \"my other column\","
                + "  \"name\" : \"my column\","
                + "  \"replace\" : false"
                + "} ]"
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnMoveOperation.class), json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testForward() throws OperationException, ParsingException {
        Operation operation = new ColumnMoveOperation("foo", 1, null);

        ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("foo"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("v1", null), new Cell("d", null)));
    }

    @Test
    public void testSamePosition() throws OperationException, ParsingException {
        Operation SUT = new ColumnMoveOperation("bar", 1, null);

        ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("a", null), new Cell("d", null)));
    }

    @Test
    public void testBackward() throws OperationException, ParsingException {
        ColumnMoveOperation SUT = new ColumnMoveOperation("hello", 1, null);

        ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("hello"), new ColumnMetadata("bar")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("d", null), new Cell("a", null)));
    }

    @Test
    public void testForwardWithAfterColumn() throws OperationException, ParsingException {
        Operation operation = new ColumnMoveOperation("foo", null, "bar");

        ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("foo"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("v1", null), new Cell("d", null)));
    }

    @Test
    public void testSamePositionWithAfterColumn() throws OperationException, ParsingException {
        Operation SUT = new ColumnMoveOperation("bar", null, "foo");

        ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("v1", null), new Cell("a", null), new Cell("d", null)));
    }

    @Test
    public void testFirstColumn() throws OperationException, ParsingException {
        ColumnMoveOperation SUT = new ColumnMoveOperation("bar", null, null);

        ChangeResult changeResult = SUT.apply(initialState, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);
        Grid applied = changeResult.getGrid();
        List<IndexedRow> rows = applied.collectRows();
        Assert.assertEquals(applied.getColumnModel().getColumns(),
                Arrays.asList(new ColumnMetadata("bar"), new ColumnMetadata("foo"), new ColumnMetadata("hello")));
        Assert.assertEquals(rows.get(0).getRow().getCells(),
                Arrays.asList(new Cell("a", null), new Cell("v1", null), new Cell("d", null)));
    }

    @Test(expectedExceptions = OperationException.class)
    public void testColumnDoesNotExist() throws OperationException, ParsingException {
        Operation SUT = new ColumnMoveOperation("not_found", 1, null);
        SUT.apply(initialState, mock(ChangeContext.class));
    }
}