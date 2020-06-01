package au.org.ala.kvs.cache;

import au.org.ala.kvs.ALAKvConfig;
import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.cache.KeyValueCache;
import org.gbif.kvs.geocode.LatLng;
import org.gbif.pipelines.kv.BitmapFactory;
import org.gbif.pipelines.parsers.config.model.KvConfig;
import org.gbif.pipelines.parsers.parsers.location.GeocodeService;
import org.gbif.rest.client.geocode.GeocodeResponse;

import lombok.SneakyThrows;

/**
 * Factory to get singleton instance of {@link GeocodeService}
 */
public class GeocodeServiceFactory {

  public static GeocodeService create(KvConfig config, ALAKvConfig alaKvConfig) {
    return GeocodeService.create(createKvStore(alaKvConfig), BitmapFactory.getInstance(config));
  }

  private static KeyValueStore<LatLng, GeocodeResponse> createKvStore(ALAKvConfig config) {
    if (config == null) {
      return null;
    }
    return GeocodeCache2kKeyValueStore.create();
  }
}
