/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.index;

import static org.apache.lucene.document.Field.Index.NOT_ANALYZED;
import static org.apache.lucene.document.Field.Index.ANALYZED;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.neo4j.kernel.api.index.ArrayEncoder.encode;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.neo4j.index.impl.lucene.LuceneDataSource;
import org.neo4j.index.impl.lucene.LuceneUtil;


class LuceneDocumentStructure
{
    private static final String NODE_ID_KEY = "id";
    private static final String STRING_PROPERTY_FIELD_IDENTIFIER = "string";
    private static final String ARRAY_PROPERTY_FIELD_IDENTIFIER = "array";
    private static final String BOOL_PROPERTY_FIELD_IDENTIFIER = "bool";
    private static final String NUMBER_PROPERTY_FIELD_IDENTIFIER = "number";
    
    Document newDocument( long nodeId )
    {
        Document document = new Document();
        document.add( field( NODE_ID_KEY, "" + nodeId, YES ) );
        return document;
    }
    
    Document newDocumentRepresentingProperty( long nodeId, Object value )
    {
        Document document = newDocument( nodeId );
        
        if ( value instanceof Number )
        {
            NumericField numberField = new NumericField( NUMBER_PROPERTY_FIELD_IDENTIFIER, NO, true );
            numberField.setDoubleValue( ((Number) value).doubleValue() );
            document.add( numberField );
        }
        else if ( value instanceof Boolean )
        {
            document.add( field( BOOL_PROPERTY_FIELD_IDENTIFIER, value.toString() ) );
        }
        else if ( value.getClass().isArray() )
        {
            document.add( field( ARRAY_PROPERTY_FIELD_IDENTIFIER, encode( value ) ) );
        }
        else
        {
            document.add( field( STRING_PROPERTY_FIELD_IDENTIFIER, value.toString() ) );
        }

        return document;
    }
    
    Fieldable newField( String key, long value )
    {
        NumericField numberField = new NumericField( key, NO, true );
        numberField.setLongValue( value );
        return numberField;
    }
    
    Query newQuery( String key, long value )
    {
        return LuceneUtil.rangeQuery( key, value, value, true, true );
    }

    private Field field( String fieldIdentifier, String value )
    {
        return field( fieldIdentifier, value, NO );
    }
    
    private Field field( String fieldIdentifier, String value, Field.Store store )
    {
        Field result = new Field( fieldIdentifier, value, store, ANALYZED );
        result.setOmitNorms( true );
        result.setIndexOptions( IndexOptions.DOCS_ONLY );
        return result;
    }

    public Query newQuery( Object value )
    {
    	
    	if (value instanceof Query){
    		
    		return (Query) value;
    	}
    	else if ( value instanceof Number )
        {
            Number number = (Number) value;
            return LuceneUtil.rangeQuery( NUMBER_PROPERTY_FIELD_IDENTIFIER, number.doubleValue(),
                    number.doubleValue(), true, true );
        }
        else if ( value instanceof Boolean )
        {
            return new TermQuery( new Term( BOOL_PROPERTY_FIELD_IDENTIFIER, value.toString() ) );
        }
        else if ( value.getClass().isArray() )
        {
            return new TermQuery( new Term( ARRAY_PROPERTY_FIELD_IDENTIFIER, encode( value ) ) );
        }
        else
        {

        	try {
				return new QueryParser(LuceneDataSource.LUCENE_VERSION, STRING_PROPERTY_FIELD_IDENTIFIER, LuceneDataSource.LOWER_CASE_KEYWORD_ANALYZER).parse(value.toString());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				return new TermQuery( new Term( STRING_PROPERTY_FIELD_IDENTIFIER, value.toString() ) );
			}			
        }
    }

    public Term newQueryForChangeOrRemove( long nodeId )
    {
        return new Term( NODE_ID_KEY, "" + nodeId );
    }

    public long getNodeId( Document from )
    {
        return Long.parseLong( from.get( NODE_ID_KEY ) );
    }
}
