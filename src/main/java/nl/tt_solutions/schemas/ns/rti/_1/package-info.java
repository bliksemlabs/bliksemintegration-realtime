@javax.xml.bind.annotation.XmlSchema(namespace = "http://www.tt-solutions.nl/schemas/NS/RTI/1.1/")
@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(type=DateTime.class,
                value=JodaDateTimeAdapter.class),
        @XmlJavaTypeAdapter(type=LocalDate.class,
                value=JodaLocalDateAdapter.class),
})
package nl.tt_solutions.schemas.ns.rti._1;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;