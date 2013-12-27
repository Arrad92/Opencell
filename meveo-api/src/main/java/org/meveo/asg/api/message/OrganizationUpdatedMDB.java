package org.meveo.asg.api.message;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;

import org.codehaus.jackson.map.ObjectMapper;
import org.meveo.api.OrganizationServiceApi;
import org.meveo.api.dto.OrganizationDto;
import org.meveo.asg.api.OrganizationUpdated;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.asg.api.service.AsgIdMappingService;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoJpaForJobs;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edward P. Legaspi
 * @since Nov 4, 2013
 **/
@MessageDriven(name = "OrganizationUpdatedMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/updateOrganization"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class OrganizationUpdatedMDB implements MessageListener {

	private static Logger log = LoggerFactory
			.getLogger(OrganizationUpdatedMDB.class);

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private OrganizationServiceApi organizationServiceApi;

	@Inject
	@MeveoJpaForJobs
	protected EntityManager em;

	@Inject
	private AsgIdMappingService asgIdMappingService;

	@Override
	public void onMessage(Message msg) {
		log.debug("onMessage: {}", msg.toString());

		if (msg instanceof TextMessage) {
			processMessage((TextMessage) msg);
		}

	}

	private void processMessage(TextMessage msg) {
		try {
			String message = msg.getText();

			ObjectMapper mapper = new ObjectMapper();

			OrganizationUpdated data = mapper.readValue(message,
					OrganizationUpdated.class);

			if (data.getOrganization() != null) {
				log.debug("Updating organization with code={}", data
						.getOrganization().getCountryId());

				OrganizationDto organizationDto = new OrganizationDto();
				organizationDto.setOrganizationId(asgIdMappingService
						.getMeveoCode(em, data.getOrganization()
								.getOrganizationId(), EntityCodeEnum.ORG));
				try {
					organizationDto.setName(data.getOrganization().getNames()
							.getItemNameData().get(0).toString());
				} catch (NullPointerException e) {
					log.warn("Item name is null.");
				} catch (IndexOutOfBoundsException e) {
					log.warn("Item name is empty.");
				}
				if (data.getOrganization().getParentId() != null) {
					organizationDto.setParentId(asgIdMappingService
							.getMeveoCode(em, data.getOrganization()
									.getParentId(), EntityCodeEnum.ORG));
				}
				organizationDto.setCountryCode(data.getOrganization()
						.getCountryId());
				organizationDto.setDefaultCurrencyCode(data.getOrganization()
						.getDefaultCurrencyCode());
				organizationDto.setLanguageCode(data.getOrganization()
						.getLanguageCode());

				organizationDto.setCurrentUserId(Long.valueOf(paramBean
						.getProperty("asp.api.userId", "1")));
				organizationDto.setProviderId(Long.valueOf(paramBean
						.getProperty("asp.api.providerId", "1")));

				organizationServiceApi.update(organizationDto);
			}
		} catch (Exception e) {
			log.error("Error processing ASG message: {}", e.getMessage());
		}
	}
}
