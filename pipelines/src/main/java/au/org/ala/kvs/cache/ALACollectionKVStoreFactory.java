package au.org.ala.kvs.cache;

import au.org.ala.kvs.ALAKvConfig;
import au.org.ala.kvs.client.*;
import au.org.ala.kvs.client.retrofit.ALACollectoryServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.cache.KeyValueCache;
import org.gbif.kvs.hbase.Command;
import org.gbif.rest.client.configuration.ClientConfiguration;

import java.io.IOException;

@Slf4j
public class ALACollectionKVStoreFactory {

    private static KeyValueStore<ALACollectionLookup, ALACollectionMatch> mapDBCache = null;

    /**
     *
     * @param clientConfiguration
     * @return
     * @throws IOException
     */
    public static KeyValueStore<ALACollectionLookup, ALACollectionMatch> alaAttributionKVStore(ClientConfiguration clientConfiguration, ALAKvConfig kvConfig) throws IOException {

        ALACollectoryServiceClient wsClient = new ALACollectoryServiceClient(clientConfiguration);
        Command closeHandler = () -> {
                try {
                    wsClient.close();
                } catch (Exception e){
                    logAndThrow(e, "Unable to close");
                }
        };

        if (kvConfig.isMapDBCacheEnabled()){
            return mapDBBackedKVStore(wsClient, closeHandler, kvConfig);
        } else {
            return cache2kBackedKVStore(wsClient, closeHandler);
        }
    }

    /**
     * Builds a KV Store backed by the rest client.
     */
    private synchronized static KeyValueStore<ALACollectionLookup, ALACollectionMatch> mapDBBackedKVStore(ALACollectoryService service, Command closeHandler, ALAKvConfig kvConfig) {

        if (mapDBCache == null) {
            KeyValueStore kvs = new KeyValueStore<ALACollectionLookup, ALACollectionMatch>() {
                @Override
                public ALACollectionMatch get(ALACollectionLookup key) {
                    try {
                        return service.lookupCodes(key.getInstitutionCode(), key.getCollectionCode());
                    } catch (Exception ex) {
                        //this is can happen for bad data and this service is suspectible to http 404 due to the fact
                        // it takes URL parameters from the raw data. So log and carry on for now.
                        log.error("Error contacting the collectory service with institutionCode {} and collectionCode {} Message: {}",
                                key.getInstitutionCode(),
                                key.getCollectionCode(),
                                ex.getMessage(),
                                ex);
                    }
                    return ALACollectionMatch.EMPTY;
                }

                @Override
                public void close() throws IOException {
                    closeHandler.execute();
                }
            };
            mapDBCache = MapDBKeyValueStore.cache(kvConfig.getCacheDirectoryPath(), kvs, ALACollectionLookup.class, ALACollectionMatch.class);
        }

        return mapDBCache;
    }

    /**
     * Builds a KV Store backed by the rest client.
     */
    private static KeyValueStore<ALACollectionLookup, ALACollectionMatch> cache2kBackedKVStore(ALACollectoryService service, Command closeHandler) {

        KeyValueStore kvs = new KeyValueStore<ALACollectionLookup, ALACollectionMatch>() {
            @Override
            public ALACollectionMatch get(ALACollectionLookup key) {
                try {
                    return service.lookupCodes(key.getInstitutionCode(), key.getCollectionCode());
                } catch (Exception ex) {
                    //this is can happen for bad data and this service is suspectible to http 404 due to the fact
                    // it takes URL parameters from the raw data. So log and carry on for now.
                    log.error("Error contacting the collectory service with institutionCode {} and collectionCode {} Message: {}",
                            key.getInstitutionCode(),
                            key.getCollectionCode(),
                            ex.getMessage(),
                            ex);
                }
                return ALACollectionMatch.EMPTY;
            }

            @Override
            public void close() throws IOException {
                closeHandler.execute();
            }
        };
        return KeyValueCache.cache(kvs, 100000l, ALACollectionLookup.class, ALACollectionMatch.class);
    }

    /**
     * Wraps an exception into a {@link RuntimeException}.
     *
     * @param throwable to propagate
     * @param message to log and use for the exception wrapper
     * @return a new {@link RuntimeException}
     */
    private static RuntimeException logAndThrow(Throwable throwable, String message) {
        log.error(message, throwable);
        return new RuntimeException(throwable);
    }
}
