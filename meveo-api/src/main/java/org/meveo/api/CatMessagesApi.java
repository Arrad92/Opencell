package org.meveo.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.CatMessagesDto;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.api.dto.response.CatMessagesListDto;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.AuditableEntity;
import org.meveo.model.BaseEntity;
import org.meveo.model.BusinessEntity;
import org.meveo.model.IAuditable;
import org.meveo.model.IProvider;
import org.meveo.model.admin.User;
import org.meveo.model.billing.CatMessages;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.MultilanguageEntityService;
import org.meveo.service.catalog.impl.CatMessagesService;

public class CatMessagesApi extends BaseApi {

	@Inject
	private CatMessagesService catMessagesService;

	/**
	 Retrieves a CatMessages by code.
	 
	 @param catMessagesCode
	 @param languageCode
	 @param provider
	 @return
	 @throws MeveoApiException
	 */
	public CatMessagesDto find(String entityClass, String code, String languageCode, Provider provider)
			throws MeveoApiException {

		if (StringUtils.isBlank(entityClass)) {
			missingParameters.add("entityClass");
		}
		if (StringUtils.isBlank(code)) {
			missingParameters.add("code");
		}

		handleMissingParameters();

		MultilanguageEntityService<?> entityService = null;
		try {
			entityService = catMessagesService.getMultilanguageEntityService(entityClass);
		} catch (BusinessException e) {
			throw new EntityDoesNotExistsException(CatMessages.class, code);
		}

		BusinessEntity entity = entityService.findByCode(code, provider);

		if (entity == null) {
			throw new EntityDoesNotExistsException(CatMessages.class, code);
		}

		String messageCode = catMessagesService.getMessageCode(entity);

		List<CatMessages> messages = new ArrayList<>();
		CatMessages translation = null;
		if(!StringUtils.isBlank(languageCode)){
			translation = catMessagesService.findByCodeAndLanguage(messageCode, languageCode, provider);
			if (translation != null) {
				messages.add(translation);
			} else {
				throw new EntityDoesNotExistsException(CatMessages.class, code);
			}
		} else {
			List<CatMessages> messageList = catMessagesService.findByCode(messageCode, provider);
			if (messageList != null && !messageList.isEmpty()) {
				messages.addAll(messageList);
			}
		}

		CatMessagesDto messageDto = null;

		messageDto = new CatMessagesDto();
		messageDto.setCode(entity.getCode());
		messageDto.setDefaultDescription(entity.getDescription());
		messageDto.setEntityClass(entity.getClass().getSimpleName());
		List<LanguageDescriptionDto> translations = catMessagesToDto(messages);
		messageDto.setTranslatedDescriptions(translations);
		return messageDto;
	}

	public void remove(String entityClass, String code, String languageCode, Provider provider)
			throws MeveoApiException {

		if (StringUtils.isBlank(entityClass)) {
			missingParameters.add("entityClass");
		}
		if (StringUtils.isBlank(code)) {
			missingParameters.add("code");
		}

		handleMissingParameters();

		MultilanguageEntityService<?> entityService;
		try {
			entityService = catMessagesService.getMultilanguageEntityService(entityClass);
		} catch (BusinessException e) {
			throw new EntityDoesNotExistsException(CatMessages.class, code);
		}

		BusinessEntity entity = entityService.findByCode(code, provider);

		if (entity == null) {
			throw new EntityDoesNotExistsException(CatMessages.class, code);
		}

		String messageCode = catMessagesService.getMessageCode(entity);
		
		List<CatMessages> messages = new ArrayList<>();
		CatMessages translation = null;
		if (!StringUtils.isBlank(languageCode)) {
			translation = catMessagesService.findByCodeAndLanguage(messageCode, languageCode, provider);
			if (translation != null) {
				messages.add(translation);
			} else {
				throw new EntityDoesNotExistsException(CatMessages.class, code);
			}
		} else {
			List<CatMessages> messageList = catMessagesService.findByCode(messageCode, provider);
			if(messageList != null && !messageList.isEmpty()){
				messages.addAll(messageList);
			}
		}
		
		for (CatMessages message : messages) {
			catMessagesService.remove(message);
		}
	}

	public void createOrUpdate(CatMessagesDto postData, User currentUser) throws MeveoApiException, BusinessException {

		if (StringUtils.isBlank(postData.getEntityClass())) {
			missingParameters.add("entityClass");
		}
		if (StringUtils.isBlank(postData.getCode())) {
			missingParameters.add("code");
		}

		handleMissingParameters();

		// retrieve entity
		BusinessEntity entity = fetchBusinessEntity(postData.getEntityClass(), postData.getCode(), currentUser);

		// update default description only if it is not blank
		if (!StringUtils.isBlank(postData.getDefaultDescription())) {
			entity.setDescription(postData.getDefaultDescription());
			update(entity, currentUser);
		}
		// save translations
		saveTranslations(entity, postData.getTranslatedDescriptions(), currentUser);

	}
	
	private BusinessEntity fetchBusinessEntity(String className, String code, User currentUser) throws MeveoApiException {
		// check if entities exist
		Class<?> entityClass = catMessagesService.getEntityClass(className);
		BusinessEntity entity = null;
		
		if (entityClass != null) {
			QueryBuilder qb = new QueryBuilder(entityClass, "c", null, currentUser.getProvider());
			qb.addCriterion("code", "=", code, false);
			entity = (BusinessEntity) qb.getQuery(catMessagesService.getEntityManager()).getSingleResult();
			if (entity == null) {
				throw new EntityDoesNotExistsException(BusinessEntity.class, code);
			}
		} else {
			throw new EntityDoesNotExistsException(className, code);
		}
		
		return entity;
	}

	private void saveTranslations(BusinessEntity entity, List<LanguageDescriptionDto> translations, User currentUser)
			throws MeveoApiException, BusinessException {
		
		// loop over translations
		CatMessages message = null;
		boolean isBlankDescription = false;
		String messageCode = catMessagesService.getMessageCode(entity);
		
		for (LanguageDescriptionDto translation : translations) {
			isBlankDescription = StringUtils.isBlank(translation.getDescription());
			// check if translation exists
			message = null;
			message = catMessagesService.findByCodeAndLanguage(messageCode, translation.getLanguageCode(),
					currentUser.getProvider());
			// create/update/delete translations
			if (message != null && !isBlankDescription) { // message exists and description is not blank
				message.setDescription(translation.getDescription());
				catMessagesService.update(message, currentUser);
			} else if (message != null && isBlankDescription) { // message exists and description is blank
				catMessagesService.remove(message);
			} else if (message == null && !isBlankDescription) { // message does not exist and description is not blank
				message = new CatMessages();
				message.setMessageCode(messageCode);
				message.setLanguageCode(translation.getLanguageCode());
				message.setDescription(translation.getDescription());
				catMessagesService.create(message, currentUser);
			}
		}
	}

	public CatMessagesListDto list(Provider provider) {
		CatMessagesListDto catMessagesListDto = new CatMessagesListDto();
		List<CatMessages> catMessagesList = catMessagesService.list();

		if (catMessagesList != null && !catMessagesList.isEmpty()) {
			
			CatMessagesDto messageDto = null;
			BusinessEntity entity = null;
			Map<String, List<CatMessages>> entities = catMessagesToMap(catMessagesList);
			List<CatMessages> messageList = null;
			List<LanguageDescriptionDto> translations = null;

			for (String messageCode : entities.keySet()) {
				messageList = entities.get(messageCode);
				entity = catMessagesService.getEntityByMessageCode(messageCode);
				if (entity != null) {
					messageDto = new CatMessagesDto();
					messageDto.setCode(entity.getCode());
					messageDto.setDefaultDescription(entity.getDescription());
					messageDto.setEntityClass(entity.getClass().getSimpleName());
					translations = catMessagesToDto(messageList);
					messageDto.setTranslatedDescriptions(translations);
					catMessagesListDto.getCatMessage().add(messageDto);
				}
			}
		}
		return catMessagesListDto;
	}

	private void update(AuditableEntity entity, User currentUser) throws MeveoApiException, BusinessException {
		log.trace("start of update {} entity (id={}) ..", entity.getClass().getSimpleName(), entity.getId());

		if (entity instanceof IAuditable) {
			((IAuditable) entity).updateAudit(currentUser);
		}
		if (entity instanceof IProvider && (((IProvider) entity).getProvider() == null)) {
			((BaseEntity) entity).setProvider(currentUser.getProvider());
		}
		entity = catMessagesService.getEntityManager().merge(entity);
		log.debug("end of update {} entity (id={}).", entity.getClass().getSimpleName(), entity.getId());
	}
	
	private Map<String, List<CatMessages>> catMessagesToMap(List<CatMessages> messageList) {
		Map<String, List<CatMessages>> messageMap = null;
		String messageCode = null;
		List<CatMessages> list = null;
		for (CatMessages message : messageList) {
			if (messageMap == null) {
				messageMap = new HashMap<>();
			}
			messageCode = message.getMessageCode();
			if (!StringUtils.isBlank(messageCode)) {
				list = messageMap.get(messageCode);
				if (list == null) {
					list = new ArrayList<>();
					messageMap.put(messageCode, list);
				}
				list.add(message);
			}
		}
		return messageMap;
	}

	private List<LanguageDescriptionDto> catMessagesToDto(List<CatMessages> messageList) {
		List<LanguageDescriptionDto> translations = new ArrayList<>();
		LanguageDescriptionDto translation = null;
		for (CatMessages message : messageList) {
			translation = new LanguageDescriptionDto();
			translation.setLanguageCode(message.getLanguageCode());
			translation.setDescription(message.getDescription());
			translations.add(translation);
		}
		return translations;
	}	
}
