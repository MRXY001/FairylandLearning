package com.iwxyi.fairyland.server.Exception;

import javax.validation.ConstraintViolationException;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = { RestController.class, Controller.class })
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> convertType) {
        final String returnTypeName = returnType.getParameterType().getName();
        return !"com.example.demo.config.ZGlobalResponse".equals(returnTypeName)
                && !"org.springframework.http.ResponseEntity".equals(returnTypeName);
    }

    @SuppressWarnings("")
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        final String returnTypeName = returnType.getParameterType().getName();
        if ("void".equals(returnTypeName)) {
            return GlobalResponse.success(null);
        }
        if (!selectedContentType.includes(MediaType.APPLICATION_JSON)) {
            return body; // 返回值不是json类型
        }
        if ("com.iwxyi.fairyland.server.Exception.GlobalResponse".equals(returnTypeName)) {
            return body;
        }
        return GlobalResponse.success(body);
    }

    /**
     * 捕获以下异常：
     * throw new DemoException("msg", code);
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({ FormatedException.class })
    public <T> GlobalResponse<T> handleException(FormatedException e) {
        // log.error(Throwables.getStackTraceAsString(e)); // 暂时用不了 Throwables
        return GlobalResponse.fail(e.getMsg(), e.getCode());
    }

    /**
     * 捕获以下异常：
     * throw new RuntimeException("xxx")
     * 默认 code = null
     * !捕获之后，系统的各种详细报错也就没了
     */
    /* @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({ Throwable.class })
    public <T> GlobalResponse<T> handleThrowable(Throwable e) {
        String msg = e.getMessage();
        log.error(msg);
        // log.error(Throwables.getStackTraceAsString(e));
        // return GlobalResponse.failed(Throwables.getStackTraceAsString(e), null);
        return GlobalResponse.fail(msg);
    } */
    

    /**
     * 参数验证处理
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ConstraintViolationException.class)
    public <T> GlobalResponse<T> constraintViolationExceptionHandler(ConstraintViolationException e) {
        String str = e.getLocalizedMessage();
        int start = str.indexOf("interpolatedMessage='") + 21;
        int end = str.indexOf("', propertyPath=", start);
        String rst = str.substring(start, end);
        return GlobalResponse.fail(rst, 500);
    }
}
