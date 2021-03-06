package com.ru.devit.mediateka.models.mapper;

import com.ru.devit.mediateka.models.model.Actor;
import com.ru.devit.mediateka.models.model.Cinema;
import com.ru.devit.mediateka.models.network.ActorDetailResponse;
import com.ru.devit.mediateka.models.network.ActorNetwork;
import com.ru.devit.mediateka.models.network.ActorResponse;
import com.ru.devit.mediateka.models.network.CinemaNetwork;
import com.ru.devit.mediateka.models.network.Poster;
import com.ru.devit.mediateka.utils.FormatterUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static com.ru.devit.mediateka.utils.FormatterUtils.*;

public class ActorDetailResponseToActor {

    private final Random random = new Random();

    public Actor map(ActorDetailResponse response){
        Actor actor = new Actor();
        actor.setActorId(response.getId());
        actor.setProfilePath(response.getProfilePath());
        actor.setName(response.getName());
        actor.setBiography(response.getBiography());
        actor.setDeathDay((String) response.getDeathday());
        actor.setPlaceOfBirth(defaultValueIfNull(response.getPlaceOfBirth()));
        checkAgeThenSet(response , actor);
        generateRandomBackgroundPoster(response , actor);
        setCinemas(response , actor);
        setPosters(response , actor);
        return actor;
    }

    public List<Actor> map(ActorResponse response){
        List<Actor> actors = new ArrayList<>();
        for (ActorNetwork actorNetwork : response.getActors()){
            Actor actor = new Actor();
            actor.setName(actorNetwork.getName());
            actor.setActorId(actorNetwork.getActorId());
            actor.setProfilePath(actorNetwork.getProfilePath());
            actors.add(actor);
        }
        return actors;
    }

    private void generateRandomBackgroundPoster(ActorDetailResponse response , Actor actor){
        int backgroundPostersSize = response.getTaggedImages().getImageResults().size();
        if (backgroundPostersSize != 0){
            String backgroundPoster = response.getTaggedImages().getImageResults().get(random.nextInt(backgroundPostersSize)).getBackgroundPoster();
            actor.setProfileBackgroundPath(backgroundPoster);
        }
    }

    private void checkAgeThenSet(ActorDetailResponse response , Actor actor){
        if (response.getBirthday() == null){
            actor.setBirthDay(DEFAULT_VALUE);
            actor.setAge(DEFAULT_VALUE);
        } else {
            actor.setBirthDay(formatDate(response.getBirthday()));
            Calendar date = new GregorianCalendar(Locale.getDefault());
            int currentYear = date.get(Calendar.YEAR);
            actor.setAge(String.valueOf(currentYear - Integer.parseInt(getYearFromDate(response.getBirthday()))));
        }
    }

    private void setCinemas(ActorDetailResponse response , Actor actor){
        List<Cinema> cinemas = new ArrayList<>();
        for (CinemaNetwork cinemaNetwork : response.getMovieCredits().getCinemas()){
            Cinema cinema = new Cinema();
            cinema.setId(cinemaNetwork.getId());
            cinema.setTitle(cinemaNetwork.getTitle());
            cinema.setReleaseDate(getYearFromDate(cinemaNetwork.getReleaseDate()));
            cinema.setDescription(cinemaNetwork.getDescription());
            cinema.setPosterUrl(cinemaNetwork.getPosterUrl());
            cinema.setVoteAverage(cinemaNetwork.getVoteAverage());
            cinema.setGenres(formatGenres(cinemaNetwork.getGenreIds()));
            cinema.setCharacter(cinemaNetwork.getCharacter());
            cinemas.add(cinema);
        }
        actor.setCinemas(cinemas);
    }

    private void setPosters(ActorDetailResponse response , Actor actor){
        List<String> posterUrls = new ArrayList<>();
        for (Poster poster : response.getImagesResponse().getActorPosters()){
            posterUrls.add(poster.getPosterUrl());
        }
        actor.setPostersUrl(posterUrls);
    }
}
