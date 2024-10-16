package com.infa.groot.repo;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 
 * @author pujain
 *
 */
@Component
public class ResultSpecifications {
	
	public static Specification<GrootScanResult> scanRecordIdEquals(Long value) {
		return (root, criteriaQuery, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("scanRecord").get("id"), value);
        };
	}
	
	public static Specification<GrootScanResult> conversationIdEquals(String string) {
		return equals("conversationId", string);
	}
	
	public static Specification<GrootScanResult> promptCategoryLike(String value) {
		return containsIgnorecase("promptCategory", value);
	}
	
	public static Specification<GrootScanResult> severityEquals(PromptSeverity promptSeverity) {
		return equals("severity", promptSeverity);
	}
	
	public static Specification<GrootScanResult> responseStatusEquals(ResponseStatus responseStatus) {
		return equals("responseStatus", responseStatus);
	}
	
	
	private static Specification<GrootScanResult> containsIgnorecase(String key, String value) {
		return ((root, criteriaQuery, criteriaBuilder) -> {
			if (StringUtils.isEmpty(value)) {
				return criteriaBuilder.conjunction();
			}

			return criteriaBuilder.like(criteriaBuilder.lower(root.get(key)),
					String.format("%%%s%%", value.toLowerCase()));
		});
	}
	
	private static Specification<GrootScanResult> equals(String key, Object value) {
		return ((root, criteriaQuery, criteriaBuilder) -> {
			if (ObjectUtils.isEmpty(value)) {
				return criteriaBuilder.conjunction();
			}

			return criteriaBuilder.equal(root.get(key), value);
		});
	}

}
