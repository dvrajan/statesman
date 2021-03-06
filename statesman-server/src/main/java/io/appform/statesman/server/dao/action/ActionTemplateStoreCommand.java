package io.appform.statesman.server.dao.action;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.model.action.template.ActionTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.utils.MapperUtils;
import io.appform.statesman.server.utils.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ActionTemplateStoreCommand implements ActionTemplateStore {

    private final LookupDao<StoredActionTemplate> actionTemplateLookupDao;
    private final LoadingCache<String, Optional<ActionTemplate>> actionTemplateCache;

    @Inject
    public ActionTemplateStoreCommand(LookupDao<StoredActionTemplate> actionTemplateLookupDao) {
        this.actionTemplateLookupDao = actionTemplateLookupDao;
        log.info("Initializing cache ACTION_TEMPLATE_CACHE");
        actionTemplateCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for action for key: {}", key);
                    return getFromDb(key);
                });

    }

    public Optional<ActionTemplate> getFromDb(String actionTemplateId) {
        try {
            return actionTemplateLookupDao.get(actionTemplateId)
                    .map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


    @Override
    public Optional<ActionTemplate> create(ActionTemplate actionTemplate) {
        try {
            return actionTemplateLookupDao.save(WorkflowUtils.toDao(actionTemplate))
                    .map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<ActionTemplate> get(String actionTemplateId) {
        try {
            return actionTemplateCache.get(actionTemplateId);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public List<ActionTemplate> all() {
        return actionTemplateLookupDao.scatterGather(DetachedCriteria.forClass(StoredActionTemplate.class))
                .stream()
                .map(WorkflowUtils::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ActionTemplate> update(ActionTemplate actionTemplate) {
        try {
            boolean updated = actionTemplateLookupDao.update(actionTemplate.getTemplateId(), actionTemplateOptional -> {
                if (actionTemplateOptional.isPresent()) {
                    actionTemplateOptional.get().setActive(actionTemplate.isActive());
                    actionTemplateOptional.get().setName(actionTemplate.getName());
                    actionTemplateOptional.get().setActionType(actionTemplate.getType().name());
                    actionTemplateOptional.get().setData(MapperUtils.serialize(actionTemplate));
                }
                return actionTemplateOptional.orElse(null);
            });
            return updated ? getFromDb(actionTemplate.getTemplateId()) : Optional.empty();
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


}
