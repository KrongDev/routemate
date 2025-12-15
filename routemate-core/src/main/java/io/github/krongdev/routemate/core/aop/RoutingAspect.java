package io.github.krongdev.routemate.core.aop;

import io.github.krongdev.routemate.core.routing.RoutingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoutingAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(specificMethod, Transactional.class);

        if (transactional == null) {
            transactional = AnnotatedElementUtils.findMergedAnnotation(specificMethod.getDeclaringClass(),
                    Transactional.class);
        }

        try {
            if (transactional != null && transactional.readOnly()) {
                RoutingContext.set(RoutingContext.READ);
            } else {
                RoutingContext.set(RoutingContext.WRITE);
            }
            return joinPoint.proceed();
        } finally {
            RoutingContext.clear();
        }
    }
}
