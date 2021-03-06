package cpw.mods.forge.serverpackutility;

import com.google.common.collect.ImmutableList;
import cpw.mods.modlauncher.Launcher;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.*;

@SuppressWarnings("unchecked")
@Mod("serverpacklocatorutility")
public class UtilityMod {
    public UtilityMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClient);
    }

    private void onClient(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(Wrapper::onShowGui);
    }
    private static class Wrapper {
        private static boolean brandingHacked = false;

        private static final Supplier<String> statusMessage;
        private static final Field brandingList;

        static {
            final Class<?> brdControl = uncheck(() -> Class.forName("net.minecraftforge.fml.BrandingControl", true, Thread.currentThread().getContextClassLoader()));
            brandingList = uncheck(() -> brdControl.getDeclaredField("overCopyrightBrandings"));
            brandingList.setAccessible(true);
            Supplier<String> statMessage;
            try {
                Optional<ClassLoader> classLoader = Launcher.INSTANCE.environment().getProperty(FMLEnvironment.Keys.LOCATORCLASSLOADER.get());
                Class<?> clz = uncheck(() -> Class.forName("cpw.mods.forge.serverpacklocator.ModAccessor", true, classLoader.orElse(Thread.currentThread().getContextClassLoader())));
                Method status = uncheck(() -> clz.getMethod("status"));
                statMessage = uncheck(() -> (Supplier<String>) status.invoke(null));
            } catch (Throwable e) {
                statMessage = ()->"ServerPack: FAILED TO LOAD";
            }
            statusMessage = statMessage;
        }

        static void onShowGui(final GuiScreenEvent.DrawScreenEvent.Pre event) {
            if (brandingHacked) return;
            if (!(event.getGui() instanceof MainMenuScreen)) return;
            List<String> branding = uncheck(() -> (List<String>) brandingList.get(null));
            if (branding != null) {
                ImmutableList.Builder<String> brd = ImmutableList.builder();
                brd.addAll(branding);
                brd.add(statusMessage.get());
                uncheck(() -> brandingList.set(null, brd.build()));
                brandingHacked = true;
            }
        }
    }
}
