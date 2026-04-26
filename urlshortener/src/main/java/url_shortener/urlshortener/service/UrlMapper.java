package url_shortener.urlshortener.service;


import url_shortener.urlshortener.dto.UrlDtos.*;
import url_shortener.urlshortener.model.Url;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UrlMapper {

    @Mapping(target = "shortUrl", ignore = true) // set in service
    UrlResponse toResponse(Url url);

    @AfterMapping
    default void setShortUrl(@MappingTarget UrlResponse response, Url url) {
        // shortUrl is injected by the service after mapping
    }
}

