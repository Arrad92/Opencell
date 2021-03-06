package org.meveo.api.billing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.BaseApi;
import org.meveo.api.account.AccessApi;
import org.meveo.api.dto.account.AccessDto;
import org.meveo.api.dto.account.ApplyOneShotChargeInstanceRequestDto;
import org.meveo.api.dto.account.ApplyProductRequestDto;
import org.meveo.api.dto.billing.ActivateServicesRequestDto;
import org.meveo.api.dto.billing.ChargeInstanceOverrideDto;
import org.meveo.api.dto.billing.InstantiateServicesRequestDto;
import org.meveo.api.dto.billing.OperationServicesRequestDto;
import org.meveo.api.dto.billing.ProductDto;
import org.meveo.api.dto.billing.ServiceInstanceDto;
import org.meveo.api.dto.billing.ServiceToActivateDto;
import org.meveo.api.dto.billing.ServiceToInstantiateDto;
import org.meveo.api.dto.billing.ServiceToUpdateDto;
import org.meveo.api.dto.billing.SubscriptionDto;
import org.meveo.api.dto.billing.SubscriptionsDto;
import org.meveo.api.dto.billing.SubscriptionsListDto;
import org.meveo.api.dto.billing.TerminateSubscriptionRequestDto;
import org.meveo.api.dto.billing.TerminateSubscriptionServicesRequestDto;
import org.meveo.api.dto.billing.UpdateServicesRequestDto;
import org.meveo.api.dto.billing.WalletOperationDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.ProductChargeInstance;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.model.mediation.Access;
import org.meveo.service.billing.impl.ChargeInstanceService;
import org.meveo.service.billing.impl.OneShotChargeInstanceService;
import org.meveo.service.billing.impl.ProductInstanceService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.billing.impl.TerminationReasonService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.billing.impl.WalletTemplateService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.catalog.impl.ProductTemplateService;
import org.meveo.service.catalog.impl.ServiceTemplateService;

@Stateless
public class SubscriptionApi extends BaseApi {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private OfferTemplateService offerTemplateService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private ServiceTemplateService serviceTemplateService;

    @Inject
    private ServiceInstanceService serviceInstanceService;

    @Inject
    private OneShotChargeTemplateService oneShotChargeTemplateService;

    @Inject
    private OneShotChargeInstanceService oneShotChargeInstanceService;

    @Inject
    private TerminationReasonService terminationReasonService;

    @Inject
    private WalletTemplateService walletTemplateService;

    @Inject
    private AccessApi accessApi;

    @Inject
    private ChargeInstanceService<ChargeInstance> chargeInstanceService;

	@Inject
	private ProductTemplateService productTemplateService;
	
    @Inject
	private ProductInstanceService productInstanceService;
    

    public void create(SubscriptionDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getUserAccount())) {
            missingParameters.add("userAccount");
        }
        if (StringUtils.isBlank(postData.getOfferTemplate())) {
            missingParameters.add("offerTemplate");
        }
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getSubscriptionDate())) {
            missingParameters.add("subscriptionDate");
        }

        handleMissingParameters();

        

        if (subscriptionService.findByCode(postData.getCode()) != null) {
            throw new EntityAlreadyExistsException(Subscription.class, postData.getCode());
        }

        UserAccount userAccount = userAccountService.findByCode(postData.getUserAccount());
        if (userAccount == null) {
            throw new EntityDoesNotExistsException(UserAccount.class, postData.getUserAccount());
        }

        OfferTemplate offerTemplate = offerTemplateService.findByCode(postData.getOfferTemplate());
        if (offerTemplate == null) {
            throw new EntityDoesNotExistsException(OfferTemplate.class, postData.getOfferTemplate());
        }

        Subscription subscription = new Subscription();
        subscription.setCode(postData.getCode());
        subscription.setDescription(postData.getDescription());
        subscription.setUserAccount(userAccount);
        subscription.setOffer(offerTemplate);

        subscription.setSubscriptionDate(postData.getSubscriptionDate());
        subscription.setTerminationDate(postData.getTerminationDate());
        subscription.setEndAgreementDate(postData.getEndAgreementDate());

        // populate customFields
        try {
            populateCustomFields(postData.getCustomFields(), subscription, true);
        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        subscriptionService.create(subscription);

        if (postData.getProducts() != null) {
            for (ProductDto productDto : postData.getProducts().getProducts()) {
                if (StringUtils.isBlank(productDto.getCode())) {
                    log.warn("code is null={}", productDto);
                    continue;
                }
                ApplyProductRequestDto dto = new ApplyProductRequestDto(productDto);
                applyProduct(dto);
            }
        }
    }

    public void update(SubscriptionDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getOfferTemplate())) {
            missingParameters.add("offerTemplate");
        }
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getSubscriptionDate())) {
            missingParameters.add("subscriptionDate");
        }

        handleMissingParameters();

        

        Subscription subscription = subscriptionService.findByCode(postData.getCode());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getCode());
        }

        if (subscription.getStatus() == SubscriptionStatusEnum.RESILIATED) {
            throw new MeveoApiException("Subscription is already RESILIATED.");
        }

        if (!StringUtils.isBlank(postData.getUserAccount())) {
            UserAccount userAccount = userAccountService.findByCode(postData.getUserAccount());
            if (userAccount == null) {
                throw new EntityDoesNotExistsException(UserAccount.class, postData.getUserAccount());
            } else if (!subscription.getUserAccount().equals(userAccount)) {
                throw new InvalidParameterException("Can not change the parent account. Subscription's current parent account (user account) is "
                        + subscription.getUserAccount().getCode());
            }
            subscription.setUserAccount(userAccount);
        }
        
        OfferTemplate offerTemplate = offerTemplateService.findByCode(postData.getOfferTemplate());
        if (offerTemplate == null) {
            throw new EntityDoesNotExistsException(OfferTemplate.class, postData.getOfferTemplate());
        }

        subscription.setCode(StringUtils.isBlank(postData.getUpdatedCode()) ? postData.getCode() : postData.getUpdatedCode());
        subscription.setOffer(offerTemplate);
        subscription.setDescription(postData.getDescription());
        subscription.setSubscriptionDate(postData.getSubscriptionDate());
        subscription.setTerminationDate(postData.getTerminationDate());
        subscription.setEndAgreementDate(postData.getEndAgreementDate());

        // populate customFields
        try {
            populateCustomFields(postData.getCustomFields(), subscription, false);
        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        subscription = subscriptionService.update(subscription);

        if (postData.getProducts() != null) {
            for (ProductDto productDto : postData.getProducts().getProducts()) {
                if (StringUtils.isBlank(productDto.getCode())) {
                    log.warn("code is null={}", productDto);
                    continue;
                }
                ApplyProductRequestDto dto = new ApplyProductRequestDto(productDto);
                applyProduct(dto);
            }
        }

    }

    public void activateServices(ActivateServicesRequestDto postData, String orderNumber, boolean ignoreAlreadyActivatedError) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getSubscription())) {
            missingParameters.add("subscription");
        }
        if (postData.getServicesToActivateDto().getService() == null || postData.getServicesToActivateDto().getService().size() == 0) {
            missingParameters.add("services");
        }

        handleMissingParameters();

        

        Subscription subscription = subscriptionService.findByCode(postData.getSubscription());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription());
        }
        
        subscription = subscriptionService.refreshOrRetrieve(subscription);
        subscription.getOffer().getOfferServiceTemplates().size();

        if (subscription.getStatus() == SubscriptionStatusEnum.RESILIATED || subscription.getStatus() == SubscriptionStatusEnum.CANCELED) {
            throw new MeveoApiException("Subscription is already RESILIATED or CANCELLED.");
        }

        // check if exists
        List<ServiceToActivateDto> serviceToActivateDtos = new ArrayList<>();
        for (ServiceToActivateDto serviceToActivateDto : postData.getServicesToActivateDto().getService()) {
            if (StringUtils.isBlank(serviceToActivateDto.getSubscriptionDate())) {
                missingParameters.add("SubscriptionDate");
                handleMissingParameters();
            }
            ServiceTemplate serviceTemplate = serviceTemplateService.findByCode(serviceToActivateDto.getCode());
            if (serviceTemplate == null) {
                throw new EntityDoesNotExistsException(ServiceTemplate.class, serviceToActivateDto.getCode());
            }

            serviceToActivateDto.setServiceTemplate(serviceTemplate);
            serviceToActivateDtos.add(serviceToActivateDto);
        }

        // instantiate
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
        for (ServiceToActivateDto serviceToActivateDto : serviceToActivateDtos) {
            ServiceTemplate serviceTemplate = serviceToActivateDto.getServiceTemplate();
            log.debug("instanciateService id={} checked, quantity={}", serviceTemplate.getId(), 1);

            List<ServiceInstance> subscriptionServiceInstances = serviceInstanceService.findByCodeSubscriptionAndStatus(serviceTemplate.getCode(), subscription);
            boolean alreadyActive = false;
            boolean alreadySuspended = false;
            ServiceInstance serviceInstance = null;
            for (ServiceInstance subscriptionServiceInstance : subscriptionServiceInstances) {
                if (subscriptionServiceInstance.getStatus() != InstanceStatusEnum.CANCELED && subscriptionServiceInstance.getStatus() != InstanceStatusEnum.TERMINATED
                        && subscriptionServiceInstance.getStatus() != InstanceStatusEnum.CLOSED) {
                    if (subscriptionServiceInstance.getStatus().equals(InstanceStatusEnum.INACTIVE)) {
                        if (serviceToActivateDto.getSubscriptionDate() != null) {
                            log.warn("need date for serviceInstance with code={}", subscriptionServiceInstance.getCode());
                            // subscriptionServiceInstance.setDescription(serviceTemplate.getDescription());
                            // // Is there a need to reset it?
                            subscriptionServiceInstance.setSubscriptionDate(serviceToActivateDto.getSubscriptionDate());
                            subscriptionServiceInstance.setQuantity(serviceToActivateDto.getQuantity());
                            // Do not update existing value
                            if (orderNumber != null) {
                                subscriptionServiceInstance.setOrderNumber(orderNumber);
                            }
                            serviceInstance = subscriptionServiceInstance;
                            serviceInstances.add(serviceInstance);
                        }
                    } else if (subscriptionServiceInstance.getStatus().equals(InstanceStatusEnum.ACTIVE)) {
                        serviceInstance = subscriptionServiceInstance;
                        alreadyActive = true;

                    } else {
                        alreadySuspended = true;
                    }
                    break;
                }

            }

            if (alreadyActive && !ignoreAlreadyActivatedError) {
                throw new MeveoApiException("ServiceInstance with code=" + serviceToActivateDto.getCode() + " must not be ACTIVE.");
            }

            if (alreadySuspended) {
                throw new MeveoApiException("ServiceInstance with code=" + serviceToActivateDto.getCode() + " must not be SUSPENDED.");
            }

            // Instantiate if it was not instantiated earlier
            if (serviceInstance == null) {
                serviceInstance = new ServiceInstance();
                serviceInstance.setCode(serviceTemplate.getCode());
                serviceInstance.setDescription(serviceTemplate.getDescription());
                serviceInstance.setServiceTemplate(serviceTemplate);
                serviceInstance.setSubscription(subscription);

                if (serviceToActivateDto.getSubscriptionDate() == null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    serviceInstance.setSubscriptionDate(calendar.getTime());
                } else {
                    serviceInstance.setSubscriptionDate(serviceToActivateDto.getSubscriptionDate());
                }
                serviceInstance.setQuantity(serviceToActivateDto.getQuantity());
                serviceInstance.setOrderNumber(orderNumber);
                
                // populate customFields
                try {
                    populateCustomFields(serviceToActivateDto.getCustomFields(), serviceInstance, true);
                } catch (MissingParameterException e) {
                    log.error("Failed to associate custom field instance to an entity: {} {}", serviceToActivateDto.getCode(), e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to associate custom field instance to an entity {}",serviceToActivateDto.getCode(), e);
                    throw new MeveoApiException("Failed to associate custom field instance to an entity " + serviceToActivateDto.getCode());
                }
                
                try {
                    serviceInstanceService.serviceInstanciation(serviceInstance);

                    if (serviceToActivateDto.getSubscriptionDate() != null) {
                        serviceInstances.add(serviceInstance);
                    }
                } catch (BusinessException e) {
                    log.error("Failed to instantiate a service {} on subscription {}", serviceToActivateDto.getCode(), subscription.getCode(), e);
                    throw new BusinessApiException(e.getMessage());
                }
            }
            
            // override price and description
            if (serviceToActivateDto.getChargeInstanceOverrides() != null && serviceToActivateDto.getChargeInstanceOverrides().getChargeInstanceOverride() != null) {
                for (ChargeInstanceOverrideDto chargeInstanceOverrideDto : serviceToActivateDto.getChargeInstanceOverrides().getChargeInstanceOverride()) {
                    if (!StringUtils.isBlank(chargeInstanceOverrideDto.getChargeInstanceCode())) {
                        ChargeInstance chargeInstance = chargeInstanceService.findByCodeAndService(chargeInstanceOverrideDto.getChargeInstanceCode(), subscription.getId(), InstanceStatusEnum.INACTIVE);
                        if (chargeInstance == null) {
                            throw new EntityDoesNotExistsException(ChargeInstance.class, chargeInstanceOverrideDto.getChargeInstanceCode());
                        }
                        if (chargeInstance.getChargeTemplate().getAmountEditable() != null && chargeInstance.getChargeTemplate().getAmountEditable()) {
                            if(chargeInstanceOverrideDto.getAmountWithoutTax()!=null){
                            	log.debug("override AmountWithoutTax:{}", chargeInstanceOverrideDto.getAmountWithoutTax());
                            	chargeInstance.setAmountWithoutTax(chargeInstanceOverrideDto.getAmountWithoutTax());
                            }
                            if (!appProvider.isEntreprise() && chargeInstanceOverrideDto.getAmountWithTax()!=null) {
                            	log.debug("override AmountWithTax:{}", chargeInstanceOverrideDto.getAmountWithTax());
                                chargeInstance.setAmountWithTax(chargeInstanceOverrideDto.getAmountWithTax());
                            }
                            if(!StringUtils.isBlank(chargeInstanceOverrideDto.getDescription())){
                            	log.debug("override description:{}", chargeInstanceOverrideDto.getDescription());
                            	chargeInstance.setDescription(chargeInstanceOverrideDto.getDescription());
                            }
                        } else{
                        	log.warn("Charge with code {} is not overrideable", chargeInstanceOverrideDto.getChargeInstanceCode());
                        }
                    } else {
                        log.warn("chargeInstance.code and amountWithoutTax must not be null.");
                    }
                }
            }
        }

        // activate services
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (serviceInstance.getStatus() == InstanceStatusEnum.SUSPENDED) {
                throw new MeveoApiException("Service " + serviceInstance.getCode() + " is Suspended");
            }

            if (serviceInstance.getStatus() == InstanceStatusEnum.ACTIVE) {
                throw new MeveoApiException("Service " + serviceInstance.getCode() + " is already Active");
            }

            OfferTemplate offer = serviceInstance.getSubscription().getOffer();
            if (offer != null && !offer.containsServiceTemplate(serviceInstance.getServiceTemplate())) {
                throw new MeveoApiException("Service " + serviceInstance.getCode() + " is not associated with Offer");
            }

            try {
                serviceInstanceService.serviceActivation(serviceInstance, null, null);
            } catch (BusinessException e) {
                throw new MeveoApiException(e.getMessage());
            }
        }

    }

    public void instantiateServices(InstantiateServicesRequestDto postData) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getSubscription())) {
            missingParameters.add("subscription");
        }
        if (postData.getServicesToInstantiate().getService() == null || postData.getServicesToInstantiate().getService().size() == 0) {
            missingParameters.add("services");
        }

        handleMissingParameters();

        

        Subscription subscription = subscriptionService.findByCode(postData.getSubscription());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription());
        }

        if (subscription.getStatus() == SubscriptionStatusEnum.RESILIATED || subscription.getStatus() == SubscriptionStatusEnum.CANCELED) {
            throw new MeveoApiException("Subscription is already RESILIATED or CANCELLED.");
        }

        // check if exists
        List<ServiceToInstantiateDto> serviceToInstantiateDtos = new ArrayList<>();
        for (ServiceToInstantiateDto serviceToInstantiateDto : postData.getServicesToInstantiate().getService()) {
            ServiceTemplate serviceTemplate = serviceTemplateService.findByCode(serviceToInstantiateDto.getCode());
            if (serviceTemplate == null) {
                throw new EntityDoesNotExistsException(ServiceTemplate.class, serviceToInstantiateDto.getCode());
            }

            serviceToInstantiateDto.setServiceTemplate(serviceTemplate);
            serviceToInstantiateDtos.add(serviceToInstantiateDto);
        }

        // instantiate
        for (ServiceToInstantiateDto serviceToInstantiateDto : serviceToInstantiateDtos) {
            ServiceTemplate serviceTemplate = serviceToInstantiateDto.getServiceTemplate();
            log.debug("instanciateService id={} checked, quantity={}", serviceTemplate.getId(), 1);

            ServiceInstance serviceInstance = null;
            List<ServiceInstance> subscriptionServiceInstances = serviceInstanceService.findByCodeSubscriptionAndStatus(serviceTemplate.getCode(), subscription);
            boolean alreadyinstanciated = false;
            for (ServiceInstance subscriptionServiceInstance : subscriptionServiceInstances) {
                if (subscriptionServiceInstance.getStatus() != InstanceStatusEnum.CANCELED && subscriptionServiceInstance.getStatus() != InstanceStatusEnum.TERMINATED
                        && subscriptionServiceInstance.getStatus() != InstanceStatusEnum.CLOSED) {
                    alreadyinstanciated = true;
                    break;
                }

            }

            if (alreadyinstanciated) {
                throw new MeveoApiException("ServiceInstance with code=" + serviceToInstantiateDto.getCode() + " must instanciated.");
            }
            if (serviceInstance == null) {
                serviceInstance = new ServiceInstance();
                serviceInstance.setCode(serviceTemplate.getCode());
                serviceInstance.setDescription(serviceTemplate.getDescription());
                serviceInstance.setServiceTemplate(serviceTemplate);
                serviceInstance.setSubscription(subscription);

                if (serviceToInstantiateDto.getSubscriptionDate() == null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    serviceInstance.setSubscriptionDate(calendar.getTime());
                } else {
                    serviceInstance.setSubscriptionDate(serviceToInstantiateDto.getSubscriptionDate());
                }
                serviceInstance.setQuantity(serviceToInstantiateDto.getQuantity());

                // populate customFields
                try {
                    populateCustomFields(serviceToInstantiateDto.getCustomFields(), serviceInstance, true);
                } catch (MissingParameterException e) {
                    log.error("Failed to associate custom field instance to an entity: {} {}", serviceToInstantiateDto.getCode(), e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to associate custom field instance to an entity {}",serviceToInstantiateDto.getCode(), e);
                    throw new MeveoApiException("Failed to associate custom field instance to an entity " + serviceToInstantiateDto.getCode());
                }
                

                try {
                    serviceInstanceService.serviceInstanciation(serviceInstance);

                } catch (BusinessException e) {
                    log.error("Failed to instantiate a service {} on subscription {}", serviceToInstantiateDto.getCode(), subscription.getCode(), e);
                    throw new BusinessApiException(e.getMessage());
                }
                
            }
        }
    }

    public void applyOneShotChargeInstance(ApplyOneShotChargeInstanceRequestDto postData) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getOneShotCharge())) {
            missingParameters.add("oneShotCharge");
        }
        if (StringUtils.isBlank(postData.getSubscription())) {
            missingParameters.add("subscription");
        }
        if (postData.getOperationDate() == null) {
            missingParameters.add("operationDate");
        }

        handleMissingParameters();

        

        OneShotChargeTemplate oneShotChargeTemplate = oneShotChargeTemplateService.findByCode(postData.getOneShotCharge());
        if (oneShotChargeTemplate == null) {
            throw new EntityDoesNotExistsException(OneShotChargeTemplate.class, postData.getOneShotCharge());
        }

        Subscription subscription = subscriptionService.findByCode(postData.getSubscription());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription());
        }

        if (subscription.getStatus() == SubscriptionStatusEnum.RESILIATED || subscription.getStatus() == SubscriptionStatusEnum.CANCELED) {
            throw new MeveoApiException("Subscription is already RESILIATED or CANCELLED.");
        }
        if (postData.getWallet() != null) {
            WalletTemplate walletTemplate = walletTemplateService.findByCode(postData.getWallet());
            if (walletTemplate == null) {
                throw new EntityDoesNotExistsException(WalletTemplate.class, postData.getWallet());
            }

            if ((!postData.getWallet().equals("PRINCIPAL")) && !subscription.getUserAccount().getPrepaidWallets().containsKey(postData.getWallet())) {
                if (postData.getCreateWallet() != null && postData.getCreateWallet()) {
                    subscription.getUserAccount().getWalletInstance(postData.getWallet());
                } else {
                    throw new MeveoApiException("Wallet " + postData.getWallet() + " is not attached to the user account, but were instructed not to create it");
                }
            }
        }

        try {            
            oneShotChargeInstanceService.oneShotChargeApplication(subscription, (OneShotChargeTemplate) oneShotChargeTemplate, postData.getWallet(), postData.getOperationDate(),
                postData.getAmountWithoutTax(), postData.getAmountWithTax(), postData.getQuantity(), postData.getCriteria1(), postData.getCriteria2(), postData.getCriteria3(),
                postData.getDescription(),null, true);
        } catch (BusinessException e) {
            throw new MeveoApiException(e.getMessage());
        }
    }
    
	public List<WalletOperationDto> applyProduct(ApplyProductRequestDto postData) throws MeveoApiException, BusinessException {
		List<WalletOperationDto> result = new ArrayList<>();
		if (StringUtils.isBlank(postData.getProduct())) {
			missingParameters.add("product");
		}
		if (StringUtils.isBlank(postData.getSubscription())) {
			missingParameters.add("subscription");
		}
		if (postData.getOperationDate() == null) {
			missingParameters.add("operationDate");
		}

		handleMissingParameters();

		

		ProductTemplate productTemplate = productTemplateService.findByCode(postData.getProduct());
		if (productTemplate == null) {
			throw new EntityDoesNotExistsException(ProductTemplate.class, postData.getProduct());
		}

		Subscription subscription = subscriptionService.findByCode(postData.getSubscription());
		if (subscription == null) {
			throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription());
		}

		if ((subscription.getStatus() != SubscriptionStatusEnum.ACTIVE) 
				&& (subscription.getStatus() != SubscriptionStatusEnum.CREATED)) {
			throw new MeveoApiException("subscription is not ACTIVE or CREATED: ["+subscription.getStatus()+"]");
		}

		List<WalletOperation> walletOperations = null;

		try {
			ProductInstance productInstance = new ProductInstance(null, subscription, productTemplate, postData.getQuantity(), postData.getOperationDate(), postData.getProduct(),
					StringUtils.isBlank(postData.getDescription()) ? productTemplate.getDescriptionOrCode() : postData.getDescription(), null);

			// populate customFields
			try {
				populateCustomFields(postData.getCustomFields(), productInstance, true);
			} catch (MissingParameterException e) {
				log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
				throw e;
			}

		    productInstanceService.instantiateProductInstance(productInstance, postData.getCriteria1(), postData.getCriteria2(), postData.getCriteria3(), false);

			walletOperations = productInstanceService.applyProductInstance(productInstance, postData.getCriteria1(), postData.getCriteria2(), postData.getCriteria3(),
					true, false);
			for (WalletOperation walletOperation : walletOperations) {
				result.add(new WalletOperationDto(walletOperation));
			}
		} catch (BusinessException e) {
			throw new MeveoApiException(e.getMessage());
		}
		
		return result;
	}


    public void terminateSubscription(TerminateSubscriptionRequestDto postData, String orderNumber) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getSubscriptionCode())) {
            missingParameters.add("subscriptionCode");
        }
        if (StringUtils.isBlank(postData.getTerminationReason())) {
            missingParameters.add("terminationReason");
        }
        if (postData.getTerminationDate() == null) {
            missingParameters.add("terminationDate");
        }

        handleMissingParameters();

        

        Subscription subscription = subscriptionService.findByCode(postData.getSubscriptionCode());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscriptionCode());
        }

        if (subscription.getStatus() == SubscriptionStatusEnum.RESILIATED) {
            throw new MeveoApiException("Subscription is already RESILIATED.");
        }

        SubscriptionTerminationReason subscriptionTerminationReason = terminationReasonService.findByCode(postData.getTerminationReason());
        if (subscriptionTerminationReason == null) {
            throw new EntityDoesNotExistsException(SubscriptionTerminationReason.class, postData.getTerminationReason());
        }

        try {
            subscriptionService.terminateSubscription(subscription, postData.getTerminationDate(), subscriptionTerminationReason, orderNumber);
        } catch (BusinessException e) {
            log.error("error while setting subscription termination", e);
            throw new MeveoApiException(e.getMessage());
        }
    }

    public void terminateServices(TerminateSubscriptionServicesRequestDto postData, String orderNumber) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getSubscriptionCode())) {
            missingParameters.add("subscriptionCode");
        }
        if (postData.getServices() == null || postData.getServices().size() == 0) {
            missingParameters.add("services");
        }
        if (StringUtils.isBlank(postData.getTerminationReason())) {
            missingParameters.add("terminationReason");
        }
        if (postData.getTerminationDate() == null) {
            missingParameters.add("terminationDate");
        }

        handleMissingParameters();

        

        Subscription subscription = subscriptionService.findByCode(postData.getSubscriptionCode());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscriptionCode());
        }

        SubscriptionTerminationReason serviceTerminationReason = terminationReasonService.findByCode(postData.getTerminationReason());
        if (serviceTerminationReason == null) {
            throw new EntityDoesNotExistsException(SubscriptionTerminationReason.class, postData.getTerminationReason());
        }

        for (String serviceInstanceCode : postData.getServices()) {
            ServiceInstance serviceInstance = serviceInstanceService.findActivatedByCodeAndSubscription(serviceInstanceCode, subscription);
            if (serviceInstance != null) {
                try {
                    serviceInstanceService.terminateService(serviceInstance, postData.getTerminationDate(), serviceTerminationReason, orderNumber);
                } catch (BusinessException e) {
                    log.error("service termination={}", e.getMessage());
                    throw new MeveoApiException(e.getMessage());
                }
            } else {
                throw new MeveoApiException("ServiceInstance with code=" + serviceInstanceCode + " must be ACTIVE.");
            }
        }
    }

    public SubscriptionsDto listByUserAccount(String userAccountCode) throws MeveoApiException {
        if (StringUtils.isBlank(userAccountCode)) {
            missingParameters.add("userAccountCode");
            handleMissingParameters();
        }

        UserAccount userAccount = userAccountService.findByCode(userAccountCode);
        if (userAccount == null) {
            throw new EntityDoesNotExistsException(UserAccount.class, userAccountCode);
        }

        SubscriptionsDto result = new SubscriptionsDto();
        List<Subscription> subscriptions = subscriptionService.listByUserAccount(userAccount);
        if (subscriptions != null) {
            for (Subscription s : subscriptions) {
                result.getSubscription().add(subscriptionToDto(s));
            }
        }

        return result;

    }

    public SubscriptionsListDto listAll(int pageSize, int pageNum) throws MeveoApiException {

        SubscriptionsListDto result = new SubscriptionsListDto();
        Map<String, Object> filters = new HashMap<>();
        PaginationConfiguration paginationConfiguration = new PaginationConfiguration(pageNum, pageSize, filters, null, null, "code", null);
        List<Subscription> subscriptions = subscriptionService.list(paginationConfiguration);
        if (subscriptions != null) {
            for (Subscription subscription : subscriptions) {
                result.getSubscription().add(subscriptionToDto(subscription));
            }
        }

        return result;

    }

    public SubscriptionDto findSubscription(String subscriptionCode) throws MeveoApiException {
        SubscriptionDto result = new SubscriptionDto();

        if (StringUtils.isBlank(subscriptionCode)) {
            missingParameters.add("subscriptionCode");
            handleMissingParameters();
        }
        Subscription subscription = subscriptionService.findByCode(subscriptionCode);
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, subscriptionCode);
        }

        result = subscriptionToDto(subscription);

        return result;
    }

    /**
     * Create or update Subscription based on subscription code
     * 
     * @param postData

     * @throws MeveoApiException
     * @throws BusinessException
     */
    public void createOrUpdate(SubscriptionDto postData) throws MeveoApiException, BusinessException {
        if (subscriptionService.findByCode(postData.getCode()) == null) {
            create(postData);
        } else {
            update(postData);
        }
    }

    public SubscriptionDto subscriptionToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setCode(subscription.getCode());
        dto.setDescription(subscription.getDescription());
        dto.setStatus(subscription.getStatus());
        dto.setStatusDate(subscription.getStatusDate());

        if (subscription.getUserAccount() != null) {
            dto.setUserAccount(subscription.getUserAccount().getCode());
        }

        if (subscription.getOffer() != null) {
            dto.setOfferTemplate(subscription.getOffer().getCode());
        }

        dto.setSubscriptionDate(subscription.getSubscriptionDate());
        dto.setTerminationDate(subscription.getTerminationDate());
        dto.setEndAgreementDate(subscription.getEndAgreementDate());

        if (subscription.getAccessPoints() != null) {
            for (Access ac : subscription.getAccessPoints()) {
                dto.getAccesses().getAccess().add(new AccessDto(ac, entityToDtoConverter.getCustomFieldsDTO(ac)));
            }
        }

        dto.setCustomFields(entityToDtoConverter.getCustomFieldsWithInheritedDTO(subscription, true));

        if (subscription.getServiceInstances() != null) {
            for (ServiceInstance serviceInstance : subscription.getServiceInstances()) {
                dto.getServices().getServiceInstance().add(new ServiceInstanceDto(serviceInstance, entityToDtoConverter.getCustomFieldsDTO(serviceInstance,true)));
            }
        }
        
		if (subscription.getProductInstances() != null) {
			for (ProductInstance productInstance : subscription.getProductInstances()) {
				for (ProductChargeInstance pci : productInstance.getProductChargeInstances()) {
					dto.getProducts().getProducts()
							.add(new ProductDto(pci, entityToDtoConverter.getCustomFieldsDTO(productInstance, true)));
				}
			}
		}

        return dto;
    }

    public void createOrUpdatePartial(SubscriptionDto subscriptionDto) throws MeveoApiException, BusinessException {

        SubscriptionDto existedSubscriptionDto = null;
        try {
            existedSubscriptionDto = findSubscription(subscriptionDto.getCode());
        } catch (Exception e) {
            existedSubscriptionDto = null;
        }

        log.debug("createOrUpdate subscription {}", subscriptionDto);
        if (existedSubscriptionDto == null) {
            create(subscriptionDto);
        } else {
            if (!StringUtils.isBlank(subscriptionDto.getTerminationDate())) {
                TerminateSubscriptionRequestDto terminateSubscriptionDto = new TerminateSubscriptionRequestDto();
                terminateSubscriptionDto.setSubscriptionCode(subscriptionDto.getCode());
                terminateSubscriptionDto.setTerminationDate(subscriptionDto.getTerminationDate());
                terminateSubscriptionDto.setTerminationReason(subscriptionDto.getTerminationReason());
                terminateSubscription(terminateSubscriptionDto, ChargeInstance.NO_ORDER_NUMBER);
                return;
            } else {

                if (!StringUtils.isBlank(subscriptionDto.getUserAccount())) {
                    existedSubscriptionDto.setUserAccount(subscriptionDto.getUserAccount());
                }
                
                if (!StringUtils.isBlank(subscriptionDto.getOfferTemplate())) {
                    existedSubscriptionDto.setOfferTemplate(subscriptionDto.getOfferTemplate());
                }

                if (!StringUtils.isBlank(subscriptionDto.getDescription())) {
                    existedSubscriptionDto.setDescription(subscriptionDto.getDescription());
                }
                if (!StringUtils.isBlank(subscriptionDto.getSubscriptionDate())) {
                    existedSubscriptionDto.setSubscriptionDate(subscriptionDto.getSubscriptionDate());
                }

                if (!StringUtils.isBlank(subscriptionDto.getEndAgreementDate())) {
                    existedSubscriptionDto.setEndAgreementDate(subscriptionDto.getEndAgreementDate());
                }

                if (!StringUtils.isBlank(subscriptionDto.getCustomFields())) {
                    existedSubscriptionDto.setCustomFields(subscriptionDto.getCustomFields());
                }
                update(existedSubscriptionDto);
            }
        }
        // accesses
        if (subscriptionDto.getAccesses() != null) {
            for (AccessDto accessDto : subscriptionDto.getAccesses().getAccess()) {
                if (StringUtils.isBlank(accessDto.getCode())) {
                    log.warn("code is null={}", accessDto);
                    continue;
                }
                if (!StringUtils.isBlank(accessDto.getSubscription()) && !accessDto.getSubscription().equalsIgnoreCase(subscriptionDto.getCode())) {
                    throw new MeveoApiException("Access's subscription " + accessDto.getSubscription() + " doesn't match with parent subscription " + subscriptionDto.getCode());
                } else {
                    accessDto.setSubscription(subscriptionDto.getCode());
                }
                accessApi.createOrUpdatePartial(accessDto);
            }
        }

        if (subscriptionDto.getServices() != null) {
            InstantiateServicesRequestDto instantiateServicesDto = new InstantiateServicesRequestDto();
            instantiateServicesDto.setSubscription(subscriptionDto.getCode());
            List<ServiceToInstantiateDto> serviceToInstantiates = instantiateServicesDto.getServicesToInstantiate().getService();

            ActivateServicesRequestDto activateServicesDto = new ActivateServicesRequestDto();
            activateServicesDto.setSubscription(subscriptionDto.getCode());
            List<ServiceToActivateDto> serviceToActivates = activateServicesDto.getServicesToActivateDto().getService();

            for (ServiceInstanceDto serviceInstanceDto : subscriptionDto.getServices().getServiceInstance()) {
                if (StringUtils.isBlank(serviceInstanceDto.getCode())) {
                    log.warn("code is null={}", serviceInstanceDto);
                    continue;
                }

                if (serviceInstanceDto.getTerminationDate() != null) {
                    TerminateSubscriptionServicesRequestDto terminateServiceDto = new TerminateSubscriptionServicesRequestDto();
                    terminateServiceDto.getServices().add(serviceInstanceDto.getCode());
                    terminateServiceDto.setSubscriptionCode(subscriptionDto.getCode());
                    terminateServiceDto.setTerminationDate(serviceInstanceDto.getTerminationDate());
                    terminateServiceDto.setTerminationReason(serviceInstanceDto.getTerminationReason());
                    terminateServices(terminateServiceDto, ChargeInstance.NO_ORDER_NUMBER);
                    continue;
                }

                if (StringUtils.isBlank(serviceInstanceDto.getSubscriptionDate())) {// instance
                                                                                    // service
                                                                                    // in
                                                                                    // sub's
                    ServiceToInstantiateDto serviceToInstantiate = new ServiceToInstantiateDto();
                    serviceToInstantiate.setCode(serviceInstanceDto.getCode());
                    serviceToInstantiate.setQuantity(serviceInstanceDto.getQuantity());
                    serviceToInstantiate.setCustomFields(serviceInstanceDto.getCustomFields());
                    serviceToInstantiates.add(serviceToInstantiate);
                } else {
                    ServiceToActivateDto serviceToActivateDto = new ServiceToActivateDto();
                    serviceToActivateDto.setCode(serviceInstanceDto.getCode());
                    serviceToActivateDto.setSubscriptionDate(serviceInstanceDto.getSubscriptionDate());
                    serviceToActivateDto.setQuantity(serviceInstanceDto.getQuantity());
                    serviceToActivateDto.setCustomFields(serviceInstanceDto.getCustomFields());
                    serviceToActivates.add(serviceToActivateDto);
                }
                if (!serviceToInstantiates.isEmpty()) {
                    try {
                        instantiateServices(instantiateServicesDto);
                    } catch (Exception e) {
                        log.error("instantiate service", e);
                    }
                    serviceToInstantiates.clear();
                }

                if (!serviceToActivates.isEmpty()) {
                    activateServices(activateServicesDto, null, true);
                    serviceToActivates.clear();
                }
            }
        }

        if (subscriptionDto.getProducts() != null) {
            for (ProductDto productDto : subscriptionDto.getProducts().getProducts()) {
                if (StringUtils.isBlank(productDto.getCode())) {
                    log.warn("code is null={}", productDto);
                    continue;
                }
                ApplyProductRequestDto dto = new ApplyProductRequestDto(productDto);
                applyProduct(dto);
            }
        }

    }

	public void suspendSubscription(String subscriptionCode, Date suspensionDate) throws MissingParameterException, EntityDoesNotExistsException, IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
		if (StringUtils.isBlank(subscriptionCode)) {
			missingParameters.add("subscriptionCode");
			handleMissingParameters();
		}
		Subscription subscription = subscriptionService.findByCode(subscriptionCode);
		if (subscription == null) {
			throw new EntityDoesNotExistsException(Subscription.class, subscriptionCode);
		}
		subscriptionService.subscriptionSuspension(subscription, suspensionDate);
	}

	/**
	 * 
	 * @param subscriptionCode
	 * @param suspensionDate
	 * @throws MissingParameterException
	 * @throws EntityDoesNotExistsException
	 * @throws IncorrectSusbcriptionException
	 * @throws IncorrectServiceInstanceException
	 * @throws BusinessException
	 */
	public void resumeSubscription(String subscriptionCode, Date suspensionDate) throws MissingParameterException, EntityDoesNotExistsException, IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
		if (StringUtils.isBlank(subscriptionCode)) {
			missingParameters.add("subscriptionCode");
			handleMissingParameters();
		}
		Subscription subscription = subscriptionService.findByCode(subscriptionCode);
		if (subscription == null) {
			throw new EntityDoesNotExistsException(Subscription.class, subscriptionCode);
		}
		subscriptionService.subscriptionReactivation(subscription, suspensionDate);
	}
	
    /**
     * 
     * @param suspendServicesRequestDto
     * @throws MissingParameterException
     * @throws EntityDoesNotExistsException
     * @throws IncorrectSusbcriptionException
     * @throws IncorrectServiceInstanceException
     * @throws BusinessException
     */
	public void suspendServices(OperationServicesRequestDto provisionningServicesRequestDto) throws MissingParameterException, EntityDoesNotExistsException, IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
		suspendOrResumeServices(provisionningServicesRequestDto, true);     
	}	
	
	public void resumeServices(OperationServicesRequestDto provisionningServicesRequestDto) throws MissingParameterException, EntityDoesNotExistsException, IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
		suspendOrResumeServices(provisionningServicesRequestDto, false);
	}	
	
	private void suspendOrResumeServices(OperationServicesRequestDto provisionningServicesRequestDto,boolean isToSuspend) throws MissingParameterException, EntityDoesNotExistsException, IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
		if (provisionningServicesRequestDto == null  ) {
			missingParameters.add("provisionningServicesRequestDto");	
			handleMissingParameters();
		}
		
		if (StringUtils.isBlank(provisionningServicesRequestDto.getSubscriptionCode())) {
			missingParameters.add("subscriptionCode");			
		}	
		for(ServiceToUpdateDto serviceToSuspendDto: provisionningServicesRequestDto.getServicesToUpdate()){
			if (StringUtils.isBlank(serviceToSuspendDto.getCode())) {
				missingParameters.add("serviceToSuspend.code");			
			}
		}
		handleMissingParameters();
		Subscription subscription = subscriptionService.findByCode(provisionningServicesRequestDto.getSubscriptionCode());
		if (subscription == null) {
			throw new EntityDoesNotExistsException(Subscription.class, provisionningServicesRequestDto.getSubscriptionCode());
		}				
		for(ServiceToUpdateDto serviceToSuspendDto: provisionningServicesRequestDto.getServicesToUpdate()){           
        	ServiceInstance serviceInstanceToSuspend = serviceInstanceService.findByCodeAndSubscription(serviceToSuspendDto.getCode(), subscription); 
        	if(serviceInstanceToSuspend == null){
        		throw new EntityDoesNotExistsException(ServiceInstance.class, serviceToSuspendDto.getCode());
        	}
        	if(isToSuspend){
        		serviceInstanceService.serviceSuspension(serviceInstanceToSuspend, serviceToSuspendDto.getActionDate());
        	}else{
        		serviceInstanceService.serviceReactivation(serviceInstanceToSuspend, serviceToSuspendDto.getActionDate());
        	}
        }	        
	}
	
	public void updateServiceInstance(UpdateServicesRequestDto postData) throws MeveoApiException, BusinessException {
		if (postData.getServicesToUpdate() == null) {
			missingParameters.add("servicesToUpdate");
		}
		if (StringUtils.isBlank(postData.getSubscriptionCode())) {
			missingParameters.add("subscriptionCode");
		}

		handleMissingParameters();

		Subscription subscription = subscriptionService.findByCode(postData.getSubscriptionCode());
		if (subscription == null) {
			throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscriptionCode());
		}

		for (ServiceToUpdateDto serviceToUpdateDto : postData.getServicesToUpdate()) {
			ServiceInstance serviceToUpdate = serviceInstanceService.findByCodeAndSubscription(serviceToUpdateDto.getCode(), subscription);
			if (serviceToUpdateDto.getEndAgreementDate() != null) {
				serviceToUpdate.setEndAgreementDate(serviceToUpdateDto.getEndAgreementDate());
			}

			// populate customFields
			try {
				populateCustomFields(serviceToUpdateDto.getCustomFields(), serviceToUpdate, false);
			} catch (MissingParameterException e) {
				log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
				throw e;
			} catch (Exception e) {
				log.error("Failed to associate custom field instance to an entity", e);
				throw e;
			}

            serviceInstanceService.update(serviceToUpdate);
		}
	}
	
}
