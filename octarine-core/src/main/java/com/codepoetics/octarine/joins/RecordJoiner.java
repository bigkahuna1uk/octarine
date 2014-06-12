package com.codepoetics.octarine.joins;

import com.codepoetics.octarine.records.Key;
import com.codepoetics.octarine.records.Record;
import com.codepoetics.octarine.records.SetKey;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class RecordJoiner<K extends Comparable<K>> {

    private final Index<K, Record> leftIndex;
    private final JoinKey<Record, K> primaryKey;

    private static final Function<Tuple2<Record, Record>, Record> mergeTuple = t -> t.first().with(t.second());
    private static Function<Tuple2<Record, Set<Record>>, Record> mergeTuples(SetKey<Record> key) {
         return t -> t.first().with(key.of(t.second()));
    }

    public RecordJoiner(Index<K, Record> leftIndex, JoinKey<Record, K> primaryKey) {
        this.leftIndex = leftIndex;
        this.primaryKey = primaryKey;
    }

    public Stream<Record> manyToOne(Collection<? extends Record> rights) {
        return manyToOne(rights.parallelStream());
    }

    public Stream<Record> manyToOne(Stream<? extends Record> rights) {
        return merge(rights, leftIndex::manyToOne).map(mergeTuple);
    }

    public Stream<Record> manyToOne(Fetcher<K, Record> fetcher) {
        return fetchAndMerge(fetcher, leftIndex::manyToOne).map(mergeTuple);
    }


    public Stream<Record> strictManyToOne(Collection<? extends Record> rights) {
        return strictManyToOne(rights.parallelStream());
    }

    public Stream<Record> strictManyToOne(Stream<? extends Record> rights) {
        return merge(rights, leftIndex::strictManyToOne).map(mergeTuple);
    }

    public Stream<Record> strictManyToOne(Fetcher<K, Record> fetcher) {
        return fetchAndMerge(fetcher, leftIndex::strictManyToOne).map(mergeTuple);
    }

    public Stream<Record> oneToMany(Collection<? extends Record> rights, SetKey<Record> manyKey) {
        return oneToMany(rights.parallelStream(), manyKey);
    }

    public Stream<Record> oneToMany(Stream<? extends Record> rights, SetKey<Record> manyKey) {
        return merge(rights, leftIndex::oneToMany).map(mergeTuples(manyKey));
    }

    public Stream<Record> oneToMany(Fetcher<K, Record> fetcher, SetKey<Record> manyKey) {
        return fetchAndMerge(fetcher, leftIndex::oneToMany).map(mergeTuples(manyKey));
    }

    public Stream<Record> strictOneToMany(Collection<? extends Record> rights, SetKey<Record> manyKey) {
        return strictOneToMany(rights.parallelStream(), manyKey);
    }

    public Stream<Record> strictOneToMany(Stream<? extends Record> rights, SetKey<Record> manyKey) {
        return merge(rights, leftIndex::strictOneToMany).map(mergeTuples(manyKey));
    }

    public Stream<Record> strictOneToMany(Fetcher<K, Record> fetcher, SetKey<Record> manyKey) {
        return fetchAndMerge(fetcher, leftIndex::strictOneToMany).map(mergeTuples(manyKey));
    }

    public Stream<Record> strictOneToOne(Collection<? extends Record> rights) {
        return strictOneToOne(rights.parallelStream());
    }

    public Stream<Record> strictOneToOne(Stream<? extends Record> rights) {
        return merge(rights, leftIndex::strictOneToOne).map(mergeTuple);
    }

    public Stream<Record> strictOneToOne(Fetcher<K, Record> fetcher) {
        return fetchAndMerge(fetcher, leftIndex::strictOneToOne).map(mergeTuple);
    }

    private <RS> Stream<Tuple2<Record, RS>> merge(Stream<? extends Record> rights, Function<Index<K, Record>, Stream<Tuple2<Record, RS>>> merger) {
        return merger.apply(primaryKey.index(rights));
    }

    private <RS> Stream<Tuple2<Record, RS>> fetchAndMerge(Fetcher<K, Record> fetcher, Function<Index<K, Record>, Stream<Tuple2<Record, RS>>> merger) {
        Collection<? extends Record> rights = fetcher.fetch(leftIndex.keys());
        return merge(rights.parallelStream(), merger);
    }
}
