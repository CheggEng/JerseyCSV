package com.test.rest;

import com.test.rest.csv.CsvObjectMapperProvider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 *
 * @author Sergey
 */
@Path("/registry")
public class MyRegistry {
    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, CsvObjectMapperProvider.TEXT_CSV, CsvObjectMapperProvider.APPLICATION_EXCEL})
    public Person[] all() {
        Person[] registry = {
                new Person("Bob", "Homeless", 60, null),
                new Person("Joe", "Doe", 30, new UsAddress("101 1st str", "San Francisco", "CA", "90000")),
                new Person("Marry", "Johnes", 29, new UsAddress("203 Main str", "San Francisco", "CA", "90001")),                
        };
        
        return registry;
    }
}