package io.appform.statesman.server.callbacktransformation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import lombok.Data;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = TransformationTemplateType.Values.ONE_SHOT,
                value = OneShotTransformationTemplate.class),
        @JsonSubTypes.Type(
                name = TransformationTemplateType.Values.STEP_BY_STEP,
                value = StepByStepTransformationTemplate.class),
})
@Data
public abstract class TransformationTemplate {

    private final String provider;
    private final TransformationTemplateType type;
    private final String idPath;
    private final TranslationTemplateType translationTemplateType;
    private final String dropDetectionRule;

    protected TransformationTemplate(
            TransformationTemplateType type,
            String idPath,
            TranslationTemplateType translationTemplateType,
            String provider,
            String dropDetectionRule) {
        this.provider = provider;
        this.type = type;
        this.idPath = idPath;
        this.translationTemplateType = translationTemplateType;
        this.dropDetectionRule = dropDetectionRule;
    }

    public abstract <T> T accept(TransformationTemplateVisitor<T> visitor);
}
