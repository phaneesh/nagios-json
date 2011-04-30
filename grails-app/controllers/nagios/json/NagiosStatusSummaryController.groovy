package nagios.json

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.converters.JSON
import grails.converters.XML

class NagiosStatusSummaryController {

  def defaultAction = "getSummary"

  def getSummary = {
    def data_to_send
    def nagios_host = params.n_host
    if(!nagios_host) {
      render(contentType:"text/json") {
          result(status : "500", data: "No Nagios Host specified")
      }
      return
    }
    //Create a new http builder with the base URL
    def statusRequest = new HTTPBuilder("http://${nagios_host}")

    //Set authentication (This might be not secure, we will have to resort to device ID based authentication)
    statusRequest.auth.basic "nagiosadmin", "adminadmin"
    //Implement the Request closure
    statusRequest.request (GET, HTML) {
      //This is for Nagios 3
      uri.path = '/cgi-bin/nagios3/status.cgi'
      //Get all status
      uri.query = [host: 'all']
      //Set User agent to simulate a browser request - to avoid unknown agent filter
      headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'



      //Implement response handler
      response.success = {resp, html ->
        def stat = processResponse(html)
        render(contentType:"text/json") {
          result(status : resp.statusLine.statusCode, data: stat)
        }
      }

      //Implement error handler
      response.failure = { resp ->
        render(contentType:"text/json") {
          result(status : resp.statusLine.statusCode, data: resp.statusLine.reasonPhrase)
        }
      }
    }
  }

  def processResponse(html) {
    def all_nodes = html.depthFirst().collect{ it }
    def obj = [:]
    obj["hosts"] = getHostsSummary(all_nodes)
    obj["service"] = getServiceSummary(all_nodes)
    obj
  }

   /*
    The nodes are matched by style and is compatible only with Nagios 3 unmodified Nagios 3
    The style selectors can be changed based on configuration if required
   */
  def getServiceSummary(flatten_nodes) {
    def obj = [:]
    def service_ok = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotalsOK"}[0]
    def service_warning = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotals"}[0]
    def service_pending = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotals"}[1]
    def service_unknown = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotalsUNKNOWN"}[0]
    def service_critical = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotalsCRITICAL"}[0]
    def service_problems = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "serviceTotalsPROBLEMS"}[0]
    obj["service_ok"] = service_ok.toString()
    obj["service_warning"] = service_warning.toString()
    obj["service_unknown"] = service_pending.toString()
    obj["service_pending"] = service_unknown.toString()
    obj["service_critical"] = service_critical.toString()
    obj["service_problems"] = service_problems.toString()
    obj
  }

  def getHostsSummary(flatten_nodes) {
    def obj = [:]
    def total_hosts_up = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "hostTotalsUP"}[0]
    def total_hosts_down = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "hostTotalsDOWN"}[0]
    def total_host_problems = flatten_nodes.findAll { n -> n.name() == "TD" && n.@class == "hostTotalsPROBLEMS"}[0]
    obj["total_hosts_up"] = total_hosts_up.toString()
    obj["total_host_problems"] = total_hosts_down.toString()
    obj["total_hosts_down"] = total_host_problems.toString()
    obj
  }
}
