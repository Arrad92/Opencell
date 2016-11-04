package org.meveo.api.dwh;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.dwh.MeasurableQuantityDto;
import org.meveo.api.dto.dwh.MeasuredValueDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.dwh.MeasurableQuantity;
import org.meveo.model.dwh.MeasuredValue;
import org.meveo.model.dwh.MeasurementPeriodEnum;
import org.meveocrm.services.dwh.MeasurableQuantityService;
import org.meveocrm.services.dwh.MeasuredValueService;

/**
 * @author Andrius Karpavicius
 **/
@Stateless
public class MeasurableQuantityApi extends BaseCrudApi<MeasurableQuantity, MeasurableQuantityDto> {

    @Inject
    private MeasurableQuantityService measurableQuantityService;
    
    @Inject
    private MeasuredValueService mvService;

    public MeasurableQuantity create(MeasurableQuantityDto postData, User currentUser) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
            handleMissingParameters();
        }

        if (measurableQuantityService.findByCode(postData.getCode(), currentUser.getProvider()) != null) {
            throw new EntityAlreadyExistsException(MeasurableQuantity.class, postData.getCode());
        }

        MeasurableQuantity measurableQuantity = fromDTO(postData, currentUser, null);
        measurableQuantityService.create(measurableQuantity, currentUser);

        return measurableQuantity;
    }

    public MeasurableQuantity update(MeasurableQuantityDto postData, User currentUser) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("measurableQuantityCode");
            handleMissingParameters();
        }

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(postData.getCode(), currentUser.getProvider());
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, postData.getCode());
        }

        measurableQuantity = fromDTO(postData, currentUser, measurableQuantity);
        measurableQuantity = measurableQuantityService.update(measurableQuantity, currentUser);

        return measurableQuantity;

    }

    public MeasurableQuantityDto find(String measurableQuantityCode, User currentUser) throws MeveoApiException {

        if (StringUtils.isBlank(measurableQuantityCode)) {
            missingParameters.add("measurableQuantityCode");
            handleMissingParameters();
        }

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(measurableQuantityCode, currentUser.getProvider());
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, measurableQuantityCode);
        }

        MeasurableQuantityDto result = new MeasurableQuantityDto(measurableQuantity);

        return result;
    }

    public void remove(String measurableQuantityCode, User currentUser) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(measurableQuantityCode)) {
            missingParameters.add("measurableQuantityCode");
            handleMissingParameters();
        }

        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(measurableQuantityCode, currentUser.getProvider());
        if (measurableQuantity == null) {
            throw new EntityDoesNotExistsException(MeasurableQuantity.class, measurableQuantityCode);
        }

        measurableQuantityService.remove(measurableQuantity, currentUser);
    }

    @Override
    public MeasurableQuantity createOrUpdate(MeasurableQuantityDto postData, User currentUser) throws MeveoApiException, BusinessException {
        MeasurableQuantity measurableQuantity = measurableQuantityService.findByCode(postData.getCode(), currentUser.getProvider());
        if (measurableQuantity == null) {
            // create
            return create(postData, currentUser);
        } else {
            // update
            return update(postData, currentUser);
        }
    }

    public List<MeasurableQuantityDto> list(String measurableQuantityCode, User currentUser) {

        List<MeasurableQuantity> measurableQuantities = null;
        if (StringUtils.isBlank(measurableQuantityCode)) {
            measurableQuantities = measurableQuantityService.list(currentUser.getProvider());
        } else {
            measurableQuantities = measurableQuantityService.findByCodeLike(measurableQuantityCode, currentUser.getProvider());
        }

        List<MeasurableQuantityDto> measurableQuantityDtos = new ArrayList<MeasurableQuantityDto>();

        for (MeasurableQuantity measurableQuantity : measurableQuantities) {
            measurableQuantityDtos.add(new MeasurableQuantityDto(measurableQuantity));
        }

        return measurableQuantityDtos;
    }

    private MeasurableQuantity fromDTO(MeasurableQuantityDto dto, User currentUser, MeasurableQuantity mqToUpdate) {

        MeasurableQuantity mq = new MeasurableQuantity();
        if (mqToUpdate != null) {
            mq = mqToUpdate;
        }

        mq.setCode(dto.getCode());
        mq.setDescription(dto.getDescription());
        mq.setTheme(dto.getTheme());
        mq.setDimension1(dto.getDimension1());
        mq.setDimension2(dto.getDimension2());
        mq.setDimension3(dto.getDimension3());
        mq.setDimension4(dto.getDimension4());
        mq.setEditable(dto.isEditable());
        mq.setAdditive(dto.isAdditive());
        mq.setSqlQuery(dto.getSqlQuery());
        mq.setMeasurementPeriod(dto.getMeasurementPeriod());
        mq.setLastMeasureDate(dto.getLastMeasureDate());

        return mq;
    }
    
	public List<MeasuredValueDto> findMVByDateAndPeriod(String code, Date fromDate, Date toDate, MeasurementPeriodEnum period, String mqCode, User currentUser)
			throws MeveoApiException {
		if (StringUtils.isBlank(code)) {
			missingParameters.add("code");
		}

		if (StringUtils.isBlank(mqCode)) {
			missingParameters.add("mqCode");
		}

		handleMissingParameters();

		MeasurableQuantity mq = measurableQuantityService.findByCode(mqCode, currentUser.getProvider());
		if (mq == null) {
			throw new EntityDoesNotExistsException(MeasurableQuantity.class, mqCode);
		}

		List<MeasuredValueDto> result = new ArrayList<>();

		List<MeasuredValue> measuredValues = mvService.getByDateAndPeriod(code, fromDate, toDate, period, mq);
		if (measuredValues != null) {
			for (MeasuredValue mv : measuredValues) {
				result.add(new MeasuredValueDto(mv));
			}
		}

		return result;
	}
	
}