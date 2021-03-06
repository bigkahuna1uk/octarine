package com.codepoetics.octarine.json;

import com.codepoetics.octarine.functional.paths.Path;
import com.codepoetics.octarine.records.Key;
import com.codepoetics.octarine.records.ListKey;
import com.codepoetics.octarine.records.Record;
import com.codepoetics.octarine.json.deserialisation.ListDeserialiser;
import com.codepoetics.octarine.json.deserialisation.MapDeserialiser;
import com.codepoetics.octarine.json.deserialisation.RecordDeserialiser;
import com.codepoetics.octarine.json.example.Address;
import com.codepoetics.octarine.json.example.Person;
import com.codepoetics.octarine.testutils.ARecord;
import com.codepoetics.octarine.testutils.AnInstance;
import com.codepoetics.octarine.testutils.Present;
import com.codepoetics.octarine.records.Valid;
import com.codepoetics.octarine.records.Validation;
import org.junit.Test;
import org.pcollections.PMap;
import org.pcollections.PVector;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static com.codepoetics.octarine.Octarine.*;
import static com.codepoetics.octarine.functional.paths.Path.toIndex;
import static com.codepoetics.octarine.testutils.IsEmptyMatcher.isEmpty;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DeserialisationTest {

    private static final Key<String> prefix = $("prefix");
    private static final Key<String> number = $("number");
    private static final RecordDeserialiser readNumber = RecordDeserialiser.builder()
            .readString(prefix)
            .readString(number)
            .get();

    private static final Key<PMap<String, Record>> numbers = $M("numbers");
    private static final RecordDeserialiser readNumbers = RecordDeserialiser.builder()
            .readMap(numbers, readNumber)
            .get();

    @Test
    public void
    deserialises_json_to_record() {
        Valid<Person> person = Person.deserialiser.validAgainst(Person.schema).fromString(String.join("\n",
                        "{",
                        "    \"name\": \"Dominic\",",
                        "    \"age\": 39,",
                        "    \"favourite colour\": \"0xFF0000\",",
                        "    \"address\": {",
                        "        \"addressLines\": [",
                        "            \"13 Rue Morgue\",",
                        "            \"PO3 1TP\"",
                        "        ]",
                        "    }",
                        "}")
        ).get();

        assertThat(person, ARecord.instance()
            .with(Person.name, "Dominic")
            .with(Person.age, 39)
            .with(Person.favouriteColour, Color.RED)
            .with(Person.address.join(Address.addressLines).join(toIndex(1)), "PO3 1TP"));
    }

    @Test
    public void
    returns_nested_validation_exceptions() {
        Validation<Person> personResult = Person.deserialiser.validAgainst(Person.schema).fromString(String.join("\n",
                "{",
                "    \"name\": \"Dominic\",",
                "    \"age\": 39,",
                "    \"favourite colour\": \"0xFF0000\",",
                "    \"address\": {",
                "        \"addressLinez\": [",
                "            \"13 Rue Morgue\",",
                "            \"PO3 1TP\"",
                "        ]",
                "    }",
                "}"));

        assertThat(personResult.isValid(), equalTo(false));
        assertThat(personResult.validationErrors(), hasItem(containsString("addressLines")));
    }

    @Test
    public void
    handles_empty_arrays() {
        assertThat(Address.deserialiser.fromString("{\"addressLines\":[]}"), ARecord.instance().with(Address.addressLines, isEmpty()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void
    handles_arrays_of_empty_arrays() {
        Key<PVector<PVector<String>>> key = $L("emptiness");
        RecordDeserialiser deserialiser = RecordDeserialiser.builder().readList(key, ListDeserialiser.readingStrings()).get();

        assertThat(deserialiser.fromString("{\"emptiness\": [[],[],[]]}"), ARecord.instance().with(key, hasItems(isEmpty(), isEmpty(), isEmpty())));
    }

    @Test
    public void
    handles_arrays_of_objects() {
        ListKey<Record> addresses = $L("addresses");
        RecordDeserialiser deserialiser = RecordDeserialiser.builder().readList(addresses, Address.deserialiser).get();
        Record r = deserialiser.fromString("{\"addresses\":[{\"addressLines\":[\"line 1\",\"line 2\"]},{\"addressLines\":[]}]}");

        assertThat(r, ARecord.instance()
                .with(addresses.join(toIndex(0)).join(Address.addressLines).join(toIndex(0)), "line 1")
                .with(addresses.join(toIndex(0)).join(Address.addressLines).join(toIndex(1)), "line 2")
                .with(addresses.join(toIndex(1)).join(Address.addressLines), isEmpty()));
    }

    @Test
    public void
    handles_arrays_of_arrays() {
        ListKey<PVector<Integer>> rows = $L("rows");
        RecordDeserialiser deserialiser = RecordDeserialiser.builder().readList(rows, ListDeserialiser.readingIntegers()).get();

        Record r = deserialiser.fromString("{\"rows\":[[1,2,3],[4,5,6],[7,8,9]]}");

        assertThat(rows.join(toIndex(1)).join(toIndex(1)).apply(r).get(), equalTo(5));
    }

    @Test
    public void
    can_deserialise_a_list_of_records() {
        String json = "[{\"prefix\": \"0208\", \"number\": \"123456\"}, {\"prefix\": \"07775\", \"number\": \"654321\"}]";

        List<Record> numbers = ListDeserialiser.readingItemsWith(readNumber).fromString(json);
        assertThat(numbers, AnInstance.<List<Record>>ofGeneric(List.class)
                .with(Path.<Record>toIndex(0).join(prefix), Present.and(equalTo("0208"))));
    }

    @Test
    public void
    can_deserialise_a_map_of_records() {
        String json = "{\"home\": {\"prefix\": \"0208\", \"number\": \"123456\"}, \"work\": {\"prefix\": \"07775\", \"number\": \"654321\"}}";

        Map<String, Record> numbers = MapDeserialiser.readingValuesWith(readNumber).fromString(json);
        assertThat(numbers, AnInstance.<Map<String, Record>>ofGeneric(Map.class)
                .with(Path.<String, Record>toKey("home").join(prefix), Present.and(equalTo("0208"))));
    }

    @Test
    public void
    can_deserialise_a_map_of_maps_of_records() {
        String json = "{\"no-one\": {}, " +
                "\"dominic\": {\"home\": {\"prefix\": \"0208\", \"number\": \"123456\"}, " +
                "\"work\": {\"prefix\": \"07775\", \"number\": \"654321\"}}}";

        Map<String, PMap<String, Record>> numbers = MapDeserialiser.readingValuesWith(MapDeserialiser.readingValuesWith(readNumber)).fromString(json);
        assertThat(numbers, AnInstance.<Map<String, PMap<String, Record>>>ofGeneric(Map.class)
                .with(Path.<String, PMap<String, Record>>toKey("dominic").join(Path.<String, Record>toKey("home")).join(prefix), Present.and(equalTo("0208"))));
    }

    @Test
    public void
    can_deserialise_a_record_containing_a_map_of_records() {
        String json = "{\"numbers\": {\"home\": {\"prefix\": \"0208\", \"number\": \"123456\"}, \"work\": {\"prefix\": \"07775\", \"number\": \"654321\"}}}";

        Record theNumbers = readNumbers.fromString(json);
        assertThat(theNumbers, ARecord.instance()
                .with(numbers.join(Path.toKey("home")).join(prefix), "0208"));
    }
}
