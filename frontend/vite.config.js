import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import basicSsl from "@vitejs/plugin-basic-ssl";
import importSideEffectPlugin from "@raquo/vite-plugin-import-side-effect";
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ command, mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    return{
  plugins: [
  scalaJSPlugin({
   // path to the directory containing the sbt build
   // default: '.'
   cwd: '..',

   // sbt project ID from within the sbt build to get fast/fullLinkJS from
   // default: the root project of the sbt build
   projectID: 'frontend',

   // URI prefix of imports that this plugin catches (without the trailing ':')
   // default: 'scalajs' (so the plugin recognizes URIs starting with 'scalajs:')
   uriPrefix: 'scalajs',
 }),
 importSideEffectPlugin({
   defNames: ["importStyle"], // see "Compact syntax" below
   rewriteModuleIds: ['**/*.css', '**/*.less'],
   verbose: true
 }),
    basicSsl({
      /** name of certification */
      name: 'test',
      /** custom trust domains */
      domains: ['localhost'],
      /** custom certification directory */
      certDir: './.devServer/cert'
    })
 ],
 // proxying to avoid CORS
 server: {
       proxy: {
         '/.hanko/': {
           target: env.HANKO_API,
           rewrite: (path) => path.replace(/^\/.hanko/, ''),
           changeOrigin: true,
         },
         '/api/': {
           target: "http://localhost:8080",
           rewrite: (path) => path.replace(/^\/api/, ''),
           changeOrigin: true,
         },
       },
     },
    }
});
