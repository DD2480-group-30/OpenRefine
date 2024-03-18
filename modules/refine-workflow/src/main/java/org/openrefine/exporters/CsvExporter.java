/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.Engine;
import org.openrefine.model.Grid;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;

public class CsvExporter implements WriterExporter {

    static CsvFormat DEFAULT_FORMAT = new CsvWriterSettings().getFormat();
    static char DEFAULT_SEPARATOR = DEFAULT_FORMAT.getDelimiter();
    static String DEFAULT_LINE_ENDING = DEFAULT_FORMAT.getLineSeparatorString();

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");
    char separator;

    public CsvExporter() {
        separator = ','; // Comma separated-value is default
    }

    public CsvExporter(char separator) {
        this.separator = separator;
    }

    private static class Configuration {

        @JsonProperty("separator")
        protected String separator = null;
        @JsonProperty("lineSeparator")
        protected String lineSeparator = DEFAULT_LINE_ENDING;
        @JsonProperty("quoteAll")
        protected boolean quoteAll = false;
    }

    @Override
    public void export(Grid grid, ProjectMetadata projectMetadata, long projectId, Map<String, String> params, Engine engine,
            final Writer writer)
            throws IOException {

        String optionsString = (params == null) ? null : params.get("options");
        Configuration options = new Configuration();
        if (optionsString != null) {
            try {
                options = ParsingUtilities.mapper.readValue(optionsString, Configuration.class);
            } catch (IOException e) {
                // Ignore and keep options null.
                e.printStackTrace();
            }
        }
        if (options.separator == null) {
            options.separator = Character.toString(separator);
        }

        final String separator = options.separator;
        final String lineSeparator = options.lineSeparator;
        final boolean quoteAll = options.quoteAll;

        final boolean printColumnHeader = (params != null && params.get("printColumnHeader") != null)
                ? Boolean.parseBoolean(params.get("printColumnHeader"))
                : true;

        CsvWriterSettings settings = new CsvWriterSettings();
        settings.setQuoteAllFields(quoteAll);
        settings.getFormat().setLineSeparator(lineSeparator);
        settings.getFormat().setDelimiter(separator);

        // Required for our test exportCsvWithQuote which wants the value "line has \"quote\""
        // to be exported as "\"line has \"\"quote\"\"", although the default of literal value
        // without the extra quoting is arguably cleaner
        settings.setEscapeUnquotedValues(true);
        settings.setQuoteEscapingEnabled(true);

        final CsvWriter csvWriter = new CsvWriter(writer, settings);

        TabularSerializer serializer = new TabularSerializer() {

            @Override
            public void startFile(JsonNode options) {
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                if (!isHeader || printColumnHeader) {
                    Stream<String> strings = cells.stream()
                            .map(cellData -> (cellData != null && cellData.text != null) ? cellData.text : "");
                    csvWriter.writeRow(strings.toArray());
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(grid, engine, params, serializer, SortingConfig.NO_SORTING);

    }

    @Override
    public String getContentType() {
        return "text/plain";
    }
}