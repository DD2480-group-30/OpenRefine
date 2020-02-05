
package org.openrefine.wikidata.qa.scrutinizers;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import org.openrefine.wikidata.qa.QAWarning;

public class CalendarScrutinizer extends ValueScrutinizer {

    public static final String earlyGregorianDateType = "early-gregorian-date";

    public static final TimeValue earliestGregorian = Datamodel.makeTimeValue(
            1582, (byte) 10, (byte) 15, (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);

    @Override
    public void scrutinize(Value value) {
        if (TimeValue.class.isInstance(value)) {
            TimeValue time = (TimeValue) value;
            if (time.getPreferredCalendarModel().equals(earliestGregorian.getPreferredCalendarModel()) &&
                    time.getPrecision() >= 10 &&
                    (time.getYear() < earliestGregorian.getYear() ||
                            time.getYear() == earliestGregorian.getYear() && time.getMonth() < earliestGregorian.getMonth() ||
                            time.getYear() == earliestGregorian.getYear() && time.getMonth() == earliestGregorian.getMonth()
                                    && time.getDay() < earliestGregorian.getDay())) {
                QAWarning warning = new QAWarning(earlyGregorianDateType, null, QAWarning.Severity.WARNING, 1);
                warning.setProperty("example_year", Long.toString(time.getYear()));
                addIssue(warning);
            }
        }
    }

}