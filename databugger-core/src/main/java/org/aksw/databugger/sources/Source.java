package org.aksw.databugger.sources;


import org.aksw.databugger.enums.TestAppliesTo;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Dimitris Kontokostas
 * Abstract class for a data source. A source can be various things like a dataset, a vocabulary or an application
 * Date: 9/16/13 1:15 PM
 */

public abstract class Source implements Comparable<Source> {
    protected static final Logger log = LoggerFactory.getLogger(Source.class);

    private final String prefix;
    private final String uri;
    private final List<SchemaSource> referencesSchemata;

    private QueryExecutionFactory queryFactory;

    public Source(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
        this.referencesSchemata = new ArrayList<SchemaSource>();
    }

    public Source(Source source) {
        this(source.getPrefix(), source.getUri());
        this.referencesSchemata.addAll(source.getReferencesSchemata());
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }

    public abstract TestAppliesTo getSourceType();

    protected abstract QueryExecutionFactory initQueryFactory();

    public QueryExecutionFactory getExecutionFactory() {
        // TODO not thread safe but minor
        if (queryFactory == null)
            queryFactory = initQueryFactory();
        return queryFactory;
    }

    public List<SchemaSource> getReferencesSchemata() {
        return referencesSchemata;
    }

    public void addReferencesSchemata(List<SchemaSource> schemata) {
        this.referencesSchemata.addAll(schemata);
    }

    @Override
    public int compareTo(Source o) {
        if (this.getPrefix().equals(o.getPrefix()) && this.getUri().equals(o.getUri()))
            return 0;
        else
            return this.getPrefix().compareTo(o.getPrefix());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Source) {
            return this.compareTo((Source) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return getPrefix() + " (" + getUri() + ")";
    }
}
