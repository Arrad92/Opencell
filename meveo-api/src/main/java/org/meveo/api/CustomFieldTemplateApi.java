package org.meveo.api;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.api.dto.CustomFieldTemplateDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.InvalidEnumValue;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class CustomFieldTemplateApi extends BaseApi {

	@Inject
	private CalendarService calendarService;

	@Inject
	private CustomFieldTemplateService customFieldTemplateService;

	public void create(CustomFieldTemplateDto postData, User currentUser) throws MeveoApiException {
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())
				&& !StringUtils.isBlank(postData.getFieldType()) && !StringUtils.isBlank(postData.getAccountLevel())
				&& !StringUtils.isBlank(postData.getStorageType())) {

			try {
				AccountLevelEnum accountLevel = AccountLevelEnum.valueOf(postData.getAccountLevel());
				if (customFieldTemplateService.findByCodeAndAccountLevel(postData.getCode(), accountLevel,
						currentUser.getProvider()) != null) {
					throw new EntityAlreadyExistsException(CustomFieldTemplate.class, postData.getCode());
				}
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(AccountLevelEnum.class.getName(), postData.getAccountLevel());
			}

			CustomFieldTemplate cf = new CustomFieldTemplate();
			cf.setCode(postData.getCode());
			cf.setDescription(postData.getDescription());
			try {
				cf.setFieldType(CustomFieldTypeEnum.valueOf(postData.getFieldType()));
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(CustomFieldTypeEnum.class.getName(), postData.getFieldType());
			}
			try {
				cf.setAccountLevel(AccountLevelEnum.valueOf(postData.getAccountLevel()));
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(AccountLevelEnum.class.getName(), postData.getAccountLevel());
			}
			cf.setDefaultValue(postData.getDefaultValue());
			try {
				cf.setStorageType(CustomFieldStorageTypeEnum.valueOf(postData.getStorageType()));
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(CustomFieldStorageTypeEnum.class.getName(), postData.getStorageType());
			}
			cf.setValueRequired(Boolean.valueOf(postData.getValueRequired()));
			cf.setVersionable(postData.isVersionable());
			cf.setTriggerEndPeriodEvent(postData.isTriggerEndPeriodEvent());

			if (!StringUtils.isBlank(postData.getCalendar())) {
				Calendar calendar = calendarService.findByCode(postData.getCalendar(), currentUser.getProvider());
				if (calendar != null) {
					cf.setCalendar(calendar);
				}
			}

			customFieldTemplateService.create(cf, currentUser, currentUser.getProvider());
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			if (StringUtils.isBlank(postData.getDescription())) {
				missingParameters.add("description");
			}
			if (StringUtils.isBlank(postData.getAccountLevel())) {
				missingParameters.add("accountLevel");
			}
			if (StringUtils.isBlank(postData.getFieldType())) {
				missingParameters.add("fieldType");
			}
			if (StringUtils.isBlank(postData.getStorageType())) {
				missingParameters.add("storageType");
			}

			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
	}
}
