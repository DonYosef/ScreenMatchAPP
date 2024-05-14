package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repoditory.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=37203a45";
    private ConvierteDatos conversor = new ConvierteDatos();

    List<DatosSerie> datosSeries = new ArrayList<>();
    List<Serie> series;
    Optional<Serie> serieBuscada;

    private  SerieRepository repository;
    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                   
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por titulo
                    5 - Top 5 mejores series
                    6 - Buscar por categoria
                    7 - Buscar por temporadas y evaluacion
                    8 - Buscar episodios por titulo
                    9 - Top 5 episodios por Serie
                                  
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    top5MejoresSeries();
                    break;
                case 6:
                    buscarSeriePorCategoria();
                    break;
                case 7:
                    buscarPorTemporadasYEvaluacion();
                    break;
                case 8:
                    buscarEpisodiosPorTitulo();
                    break;
                case 9:
                    buscarTop5Episodios();
                    break;
                case 0:
                System.out.println("Cerrando la aplicación...");
                break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }

    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Ingrese nombre de la Serie, de cual requiere los capitulos: ");
        var serieIngresada = teclado.nextLine();

        Optional<Serie> serieOptional = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(serieIngresada.toLowerCase()))
                .findFirst();

        if (serieOptional.isPresent()) {

            var serieEncontrada = serieOptional.get();

            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(),e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);

        }
    }
    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        //datosSeries.add(datos);
        Serie serie = new Serie(datos);
        repository.save(serie);
    }

    private void mostrarSeriesBuscadas() {

        series = repository.findAll();

        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);

    }
    private void buscarSeriesPorTitulo() {
        System.out.println("Ingrese nombre de la Serie que deseas buscar: ");
        var nombreSerie = teclado.nextLine();

        serieBuscada = repository.findByTituloContainsIgnoreCase(nombreSerie);

        if(serieBuscada.isPresent()){
            System.out.println("La serie buscada es "+serieBuscada.get());
        }else{
            System.out.println("Serie no encontrada");
        }
    }

    private void top5MejoresSeries() {
        List<Serie> topSeries = repository.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s -> System.out.println("Serie: " + s.getTitulo() +" Evaluación: " + s.getEvaluacion()));
    }

    public void buscarSeriePorCategoria(){
        System.out.println("Ingrese genero/categoria de la Serie que deseas buscar: ");
        var genero = teclado.nextLine();
        var categoria = Categoria.fromEspanol(genero);
        List<Serie> seriesPorCategoria  = repository.findByGenero(categoria);
        System.out.println("Las series de la categoria "+genero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarPorTemporadasYEvaluacion(){
        System.out.println("Ingrese cantidad de temporadas maximas de la Serie que deseas buscar: ");
        var temporadas = teclado.nextInt();
        teclado.nextLine();

        System.out.println("Ingrese evaluacion minima de la Serie que deseas buscar: ");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();

        List<Serie> filtroSerie = repository.seriesPorTemporadaYEvaluacion(temporadas,evaluacion);
        System.out.println("*** Series filtradas ***");

        filtroSerie.forEach(s ->
                System.out.println(s.getTitulo() + " -Evaluacion: "+s.getEvaluacion()));
    }

    private void buscarEpisodiosPorTitulo(){
        System.out.println("Ingrese el nombre del episodio que desea buscar:");
        var nombreEpisodio = teclado.nextLine();

        List<Episodio> episodiosEncontrados =  repository.episodiosPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e -> System.out.printf(" Serie %s Temporada %s Espisodio %s Evaluacion %s",
        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
    }

    private void buscarTop5Episodios(){
        buscarSeriesPorTitulo();
        if (serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios =  repository.top5Episodios(serie);
            topEpisodios.forEach(e -> System.out.printf(" Serie %s - Temporada %s - Espisodio %s - Evaluacion %s \n",
                    e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
        }
    }



}

