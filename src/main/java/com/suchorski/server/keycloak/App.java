package com.suchorski.server.keycloak;

import java.util.NoSuchElementException;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.suchorski.server.keycloak.providers.JsonProviderFactory;

import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationPath("/")
public class App extends KeycloakApplication {

	static ServerProperties properties;

	@Override
	protected void loadConfig() {
		JsonConfigProviderFactory factory = new JsonProviderFactory();
		Config.init(factory.create().orElseThrow(() -> new NoSuchElementException("No value present")));
	}

	@Override
	protected ExportImportManager bootstrap() {
		final ExportImportManager exportImportManager = super.bootstrap();
		createMasterRealmAdminUser();
		createMyRealm();
		return exportImportManager;
	}

	private void createMasterRealmAdminUser() {
		try (KeycloakSession session = getSessionFactory().create()) {
			ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
			try {
				session.getTransactionManager().begin();
				applianceBootstrap.createMasterRealmUser(properties.username(), properties.password());

				session.getTransactionManager().commit();
			} catch (Exception ex) {
				log.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
				session.getTransactionManager().rollback();
			}
		}
	}

	private void createMyRealm() {
		KeycloakSession session = getSessionFactory().create();

		try {
			session.getTransactionManager().begin();

			RealmManager manager = new RealmManager(session);
			Resource lessonRealmImportFile = new ClassPathResource("myrealm.json");

			manager.importRealm(
					JsonSerialization.readValue(lessonRealmImportFile.getInputStream(), RealmRepresentation.class));

			session.getTransactionManager().commit();
		} catch (Exception ex) {
			log.warn("Failed to import Realm json file: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}

		session.close();
	}

}
