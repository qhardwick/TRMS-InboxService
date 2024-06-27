package com.skillstorm.configs;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

@Configuration
@EnableReactiveCassandraRepositories(basePackages = {"com.skillstorm.repositories"})
public class CassandraConfig {

    @Bean
    CqlSessionFactoryBean session() {
        CqlSessionFactoryBean cqlSessionFactory = new CqlSessionFactoryBean();
        DriverConfigLoader loader = DriverConfigLoader.fromClasspath("application.conf");
        cqlSessionFactory.setSessionBuilderConfigurer(sessionBuilder -> sessionBuilder.withConfigLoader(loader).withKeyspace("trms"));
        cqlSessionFactory.setKeyspaceName("trms");

        return cqlSessionFactory;
    }

    @Bean
    public SessionFactoryFactoryBean sessionFactory(CqlSession session, CassandraConverter converter) {
        SessionFactoryFactoryBean sessionFactory = new SessionFactoryFactoryBean();
        ((MappingCassandraConverter) converter).setUserTypeResolver(new SimpleUserTypeResolver(session));
        sessionFactory.setSession(session);
        sessionFactory.setConverter(converter);
        // Auto-ddl statement: CREATE, CREATE_IF_NOT_EXISTS, NONE:
        sessionFactory.setSchemaAction(SchemaAction.CREATE_IF_NOT_EXISTS);

        return sessionFactory;
    }

    @Bean
    public CassandraConverter converter(CassandraMappingContext mappingContext) {
        return new MappingCassandraConverter(mappingContext);
    }

    @Bean
    public CassandraMappingContext mappingContext() {
        return  new CassandraMappingContext();
    }

    @Bean
    public CassandraOperations cassandraTemplate(SessionFactory sessionFactory, CassandraConverter converter) {
        return new CassandraTemplate(sessionFactory, converter);
    }

}
