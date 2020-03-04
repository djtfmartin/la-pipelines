package au.org.ala.kvs.cache;

import au.org.ala.kvs.client.*;
import au.org.ala.kvs.client.retrofit.ALACollectoryServiceClient;
import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.hbase.Command;
import org.gbif.rest.client.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ALACollectionKVStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ALACollectionKVStoreFactory.class);

    private static KeyValueStore<ALACollectionLookup, ALACollectionMatch> mapDBCache = null;

    /**
     *
     * @param clientConfiguration
     * @return
     * @throws IOException
     */
    public static KeyValueStore<ALACollectionLookup, ALACollectionMatch> alaAttributionKVStore(ClientConfiguration clientConfiguration) throws IOException {

        ALACollectoryServiceClient wsClient = new ALACollectoryServiceClient(clientConfiguration);
        Command closeHandler = () -> {
                try {
                    wsClient.close();
                } catch (Exception e){
                    logAndThrow(e, "Unable to close");
                }
        };
        KeyValueStore<ALACollectionLookup, ALACollectionMatch>  kvs = mapDBBackedKVStore(wsClient, closeHandler);
        return kvs;
    }

    /**
     * Builds a KV Store backed by the rest client.
     */
    private synchronized static KeyValueStore<ALACollectionLookup, ALACollectionMatch> mapDBBackedKVStore(ALACollectoryService service, Command closeHandler) {

        if (mapDBCache == null) {
            KeyValueStore kvs = new KeyValueStore<ALACollectionLookup, ALACollectionMatch>() {
                @Override
                public ALACollectionMatch get(ALACollectionLookup key) {
                    try {
                        return service.lookupCodes(key.getInstitutionCode(), key.getCollectionCode());
                    } catch (Exception ex) {
                        throw logAndThrow(ex, "Error contacting the collectory service");
                    }
                }

                @Override
                public void close() throws IOException {
                    closeHandler.execute();
                }
            };
            mapDBCache = MapDBKeyValueStore.cache("/data/pipelines-cache", kvs, ALACollectionLookup.class, ALACollectionMatch.class);
        }

        return mapDBCache;
    }

    /**
     * Wraps an exception into a {@link RuntimeException}.
     * @param throwable to propagate
     * @param message to log and use for the exception wrapper
     * @return a new {@link RuntimeException}
     */
    private static RuntimeException logAndThrow(Throwable throwable, String message) {
        LOG.error(message, throwable);
        return new RuntimeException(throwable);
    }
}
