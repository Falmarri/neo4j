/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.desktop.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import org.neo4j.desktop.config.OperatingSystemFamily;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.PropertyFileConfigurator;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;

import static org.neo4j.helpers.collection.MapUtil.load;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

public class DesktopConfigurator implements Configurator
{
    private final CompositeConfiguration compositeConfig = new CompositeConfiguration();

    private final Map<String, String> map = new HashMap<String, String>();

    private Configurator propertyFileConfig;

    public DesktopConfigurator()
    {
        refresh();
    }

    public void refresh() {
        compositeConfig.clear();

        compositeConfig.addConfiguration(new MapConfiguration( map ));

        // re-read server properties, then add to config
        propertyFileConfig = new PropertyFileConfigurator(getServerConfigurationFile());
        compositeConfig.addConfiguration(propertyFileConfig.configuration());
    }

    @Override
    public Configuration configuration()
    {
        return compositeConfig;
    }

    @Override
    public Map<String, String> getDatabaseTuningProperties()
    {
        return loadDatabasePropertiesFromFileInDatabaseDirectoryIfExists();
    }

    @Override
    public Set<ThirdPartyJaxRsPackage> getThirdpartyJaxRsClasses()
    {
        return propertyFileConfig.getThirdpartyJaxRsClasses();
    }

    @Override
    public Set<ThirdPartyJaxRsPackage> getThirdpartyJaxRsPackages()
    {
        return propertyFileConfig.getThirdpartyJaxRsPackages();
    }

    protected Map<String, String> loadDatabasePropertiesFromFileInDatabaseDirectoryIfExists()
    {
        try
        {
            return load( getDatabaseConfigurationFile() );
        }
        catch ( IOException e )
        {
            return stringMap();
        }
    }

    public void setDatabaseDirectory(String directory) {
        map.put( Configurator.DATABASE_LOCATION_PROPERTY_KEY, directory );
        map.put( Configurator.DB_TUNING_PROPERTY_FILE_KEY, new File(directory, "neo4j.properties").getAbsolutePath() );
    }

    public String getDatabaseDirectory() {
        return map.get( Configurator.DATABASE_LOCATION_PROPERTY_KEY );
    }

    public int getServerPort() {
        return configuration().getInt( Configurator.WEBSERVER_PORT_PROPERTY_KEY, Configurator.DEFAULT_WEBSERVER_PORT );
    }

    public File getDatabaseConfigurationFile() {
        return new File( map.get( Configurator.DB_TUNING_PROPERTY_FILE_KEY ) );
    }

    public File getServerConfigurationFile() {
        if ( OperatingSystemFamily.WINDOWS.isDetected() )
        {
            String appData = System.getenv( "APPDATA" );
            if ( null != appData )
            {
                return new File( new File ( appData ), "neo4j-server.properties" );
            }
        }
        return new File( new File ( System.getProperty( "user.home" ) ) , ".neo4j-server.properties" );
    }
}
